package linksawakening.audio.browser;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import linksawakening.audio.music.MusicCatalog;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;
import linksawakening.audio.openal.OpenAlMusicPlayer;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;

/** Standalone Swing audition view over headless transport and catalog models. */
public final class AudioBrowserFrame extends JFrame {
    private static final int TIMER_DELAY_MS = 30;

    final MusicBrowserModel browserModel;
    final SoundEffectBrowserModel soundEffectBrowserModel;
    final BrowserTransportState transportState = new BrowserTransportState();
    final OpenAlMusicPlayer player;
    final MusicDriver driver;
    final DefaultListModel<MusicTrack> trackListModel = new DefaultListModel<>();
    final JList<MusicTrack> trackList = new JList<>(trackListModel);
    final DefaultListModel<SoundEffect> soundEffectListModel = new DefaultListModel<>();
    final JList<SoundEffect> soundEffectList = new JList<>(soundEffectListModel);
    final JTextField searchField = new JTextField();
    final JTextField soundEffectSearchField = new JTextField();
    final JButton playButton = new JButton("Play");
    final JButton pauseResumeButton = new JButton("Pause");
    final JButton stopButton = new JButton("Stop");
    final JButton playSoundEffectButton = new JButton("Play SFX");
    final JCheckBox loopCheckBox = new JCheckBox("Loop");
    final JSlider volumeSlider = new JSlider(0, 100, 100);
    final JTextArea metadataArea = new JTextArea(4, 32);
    final JTextArea soundEffectMetadataArea = new JTextArea(4, 32);
    final JLabel statusLabel = new JLabel("Ready");
    final JLabel[] channelLabels = {
        new JLabel("Ch1 idle"), new JLabel("Ch2 idle"), new JLabel("Ch3 idle"), new JLabel("Ch4 idle")
    };
    final Timer updateTimer;

    public AudioBrowserFrame(
            MusicCatalog catalog, SoundEffectCatalog soundEffectCatalog, OpenAlMusicPlayer player, MusicDriver driver) {
        super("LADX Audio Browser");
        Objects.requireNonNull(catalog, "catalog");
        this.player = Objects.requireNonNull(player, "player");
        this.driver = Objects.requireNonNull(driver, "driver");
        browserModel = new MusicBrowserModel(catalog.tracks());
        soundEffectBrowserModel = new SoundEffectBrowserModel(
                Objects.requireNonNull(soundEffectCatalog, "soundEffectCatalog").effects());
        updateTimer = new Timer(TIMER_DELAY_MS, event -> updatePlayerState());

        configureWindow();
        configureTrackList();
        configureSoundEffectList();
        configureControls();
        fillTrackList(browserModel.filteredTracks(""));
        fillSoundEffectList(soundEffectBrowserModel.filteredEffects(""));
        refreshMetadata();
        refreshSoundEffectMetadata();
        refreshTransportAvailability();
        updateTimer.start();
    }

    public static void showStartupError(String message) {
        JOptionPane.showMessageDialog(
                null,
                message == null || message.isBlank() ? "Audio browser failed to start" : message,
                "LADX Audio Browser",
                JOptionPane.ERROR_MESSAGE);
    }

    private void configureWindow() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760, 420));
        setLayout(new BorderLayout(8, 8));
        add(buildSearchPanel(), BorderLayout.NORTH);
        add(buildBrowserTabs(), BorderLayout.CENTER);
        add(buildDetailsPanel(), BorderLayout.EAST);
        add(buildStatusPanel(), BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                updateTimer.stop();
                player.close();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    private void configureTrackList() {
        trackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trackList.setCellRenderer(new TrackCellRenderer());
        trackList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                refreshMetadata();
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyFilter();
            }
        });
    }

    private void configureSoundEffectList() {
        soundEffectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        soundEffectList.setCellRenderer(new SoundEffectCellRenderer());
        soundEffectList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                refreshSoundEffectMetadata();
                refreshTransportAvailability();
            }
        });
        soundEffectSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applySoundEffectFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applySoundEffectFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applySoundEffectFilter();
            }
        });
    }

    private void configureControls() {
        playButton.addActionListener(event -> playSelectedTrack());
        playSoundEffectButton.addActionListener(event -> playSelectedSoundEffect());
        pauseResumeButton.addActionListener(event -> togglePauseResume());
        stopButton.addActionListener(event -> stopPlayback());
        loopCheckBox.addActionListener(event -> player.setLoopEnabled(loopCheckBox.isSelected()));
        volumeSlider.addChangeListener(event -> player.setVolume(volumeSlider.getValue() / 100.0f));
    }

    private void applyFilter() {
        MusicTrack selected = trackList.getSelectedValue();
        fillTrackList(browserModel.filteredTracks(searchField.getText()));
        if (selected != null) {
            trackList.setSelectedValue(selected, true);
        }
        if (trackList.getSelectedIndex() < 0 && trackListModel.getSize() > 0) {
            trackList.setSelectedIndex(0);
        }
        refreshMetadata();
    }

    void applySoundEffectFilter() {
        SoundEffect selected = soundEffectList.getSelectedValue();
        fillSoundEffectList(soundEffectBrowserModel.filteredEffects(soundEffectSearchField.getText()));
        if (selected != null) {
            soundEffectList.setSelectedValue(selected, true);
        }
        if (soundEffectList.getSelectedIndex() < 0 && soundEffectListModel.getSize() > 0) {
            soundEffectList.setSelectedIndex(0);
        }
        refreshSoundEffectMetadata();
        refreshTransportAvailability();
    }

    private void fillTrackList(List<MusicTrack> tracks) {
        trackListModel.clear();
        for (MusicTrack track : tracks) {
            trackListModel.addElement(track);
        }
        if (trackListModel.getSize() > 0 && trackList.getSelectedIndex() < 0) {
            trackList.setSelectedIndex(0);
        }
    }

    private void fillSoundEffectList(List<SoundEffect> effects) {
        soundEffectListModel.clear();
        for (SoundEffect effect : effects) {
            soundEffectListModel.addElement(effect);
        }
        if (soundEffectListModel.getSize() > 0 && soundEffectList.getSelectedIndex() < 0) {
            soundEffectList.setSelectedIndex(0);
        }
    }

    private void playSelectedTrack() {
        MusicTrack track = trackList.getSelectedValue();
        if (track == null) {
            statusLabel.setText("No track selected");
            return;
        }
        player.play(track);
        if (player.isAvailable() && driver.isPlaying()) {
            transportState.playbackStarted();
        } else {
            transportState.playbackStopped();
        }
        statusLabel.setText(player.statusMessage());
        refreshChannelActivity();
        refreshTransportAvailability();
    }

    void playSelectedSoundEffect() {
        SoundEffect effect = soundEffectList.getSelectedValue();
        if (effect == null) {
            statusLabel.setText("No SFX selected");
            return;
        }
        if (transportState.playbackActive()) {
            player.stop();
            transportState.soundEffectPreviewStarted();
        }
        player.playSoundEffect(effect);
        String message = player.statusMessage();
        statusLabel.setText(message == null || message.isBlank() ? "SFX triggered" : message);
        refreshChannelActivity();
        refreshTransportAvailability();
    }

    private void togglePauseResume() {
        if (!transportState.playbackActive()) {
            return;
        }
        boolean paused = transportState.togglePauseResume();
        if (paused) {
            player.pause();
        } else {
            player.resume();
        }
        statusLabel.setText(player.statusMessage());
        refreshTransportAvailability();
    }

    private void stopPlayback() {
        player.stop();
        transportState.playbackStopped();
        statusLabel.setText(player.statusMessage());
        refreshChannelActivity();
        refreshTransportAvailability();
    }

    private void updatePlayerState() {
        player.update();
        if (!player.isAvailable() || (transportState.playbackActive() && !driver.isPlaying())) {
            transportState.playbackStopped();
        }
        refreshChannelActivity();
        refreshTransportAvailability();
        String message = player.statusMessage();
        if (message != null && !message.isBlank()) {
            statusLabel.setText(message);
        }
    }

    private void refreshMetadata() {
        MusicTrack track = trackList.getSelectedValue();
        metadataArea.setText(track == null ? "" : browserModel.metadata(track));
        metadataArea.setCaretPosition(0);
    }

    private void refreshSoundEffectMetadata() {
        SoundEffect effect = soundEffectList.getSelectedValue();
        soundEffectMetadataArea.setText(effect == null ? "" : soundEffectBrowserModel.metadata(effect));
        soundEffectMetadataArea.setCaretPosition(0);
    }

    private void refreshChannelActivity() {
        for (int i = 0; i < channelLabels.length; i++) {
            boolean active = driver.isChannelActive(i + 1);
            channelLabels[i].setText("Ch" + (i + 1) + (active ? " active" : " idle"));
        }
    }

    private void refreshTransportAvailability() {
        boolean available = player.isAvailable();
        boolean hasTrack = trackList.getSelectedValue() != null;
        TransportControls controls = transportState.controls(available, hasTrack);
        playButton.setEnabled(controls.playEnabled());
        pauseResumeButton.setEnabled(controls.pauseResumeEnabled());
        pauseResumeButton.setText(controls.pauseResumeText());
        stopButton.setEnabled(controls.stopEnabled());
        loopCheckBox.setEnabled(available);
        volumeSlider.setEnabled(available);
        playSoundEffectButton.setEnabled(player.isSoundEffectAvailable() && soundEffectList.getSelectedValue() != null);
        if (!available) {
            String message = player.statusMessage();
            statusLabel.setText(message == null || message.isBlank() ? "Audio unavailable" : message);
        }
    }

    private static final class TrackCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MusicTrack track) {
                setText(MusicBrowserModel.hexByte(track.id()) + " " + track.name());
            }
            return this;
        }
    }

    private static final class SoundEffectCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof SoundEffect effect) {
                setText(effect.namespace().name() + " "
                        + SoundEffectBrowserModel.hexByte(effect.id()) + " "
                        + effect.name());
            }
            return this;
        }
    }

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panel.add(new JLabel("Search"), BorderLayout.WEST);
        panel.add(searchField, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane buildTrackPanel() {
        JScrollPane scrollPane = new JScrollPane(trackList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Tracks"));
        return scrollPane;
    }

    private JTabbedPane buildBrowserTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Music", buildTrackPanel());
        tabs.addTab("SFX", buildSoundEffectPanel());
        return tabs;
    }

    private JPanel buildSoundEffectPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        searchPanel.add(new JLabel("Search"), BorderLayout.WEST);
        searchPanel.add(soundEffectSearchField, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.NORTH);

        JScrollPane listScrollPane = new JScrollPane(soundEffectList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Sound Effects"));
        panel.add(listScrollPane, BorderLayout.CENTER);

        JPanel detailsPanel = new JPanel(new BorderLayout(4, 4));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 6));
        detailsPanel.add(playSoundEffectButton, BorderLayout.NORTH);
        soundEffectMetadataArea.setEditable(false);
        soundEffectMetadataArea.setLineWrap(true);
        soundEffectMetadataArea.setWrapStyleWord(true);
        detailsPanel.add(new JScrollPane(soundEffectMetadataArea), BorderLayout.CENTER);
        panel.add(detailsPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        panel.add(buildTransportPanel(), BorderLayout.NORTH);

        metadataArea.setEditable(false);
        metadataArea.setLineWrap(true);
        metadataArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(metadataArea), BorderLayout.CENTER);
        panel.add(buildChannelPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildTransportPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(playButton);
        buttons.add(pauseResumeButton);
        buttons.add(stopButton);
        panel.add(buttons);
        panel.add(loopCheckBox);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        panel.add(new JLabel("Volume"));
        panel.add(volumeSlider);
        return panel;
    }

    private JPanel buildChannelPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Channels"));
        for (JLabel channelLabel : channelLabels) {
            channelLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(channelLabel);
        }
        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }
}
