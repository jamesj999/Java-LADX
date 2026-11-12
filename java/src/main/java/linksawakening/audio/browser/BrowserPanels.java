package linksawakening.audio.browser;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/** Swing panel assembly for the audition window. */
final class BrowserPanels {
    private final AudioBrowserFrame frame;

    BrowserPanels(AudioBrowserFrame frame) {
        this.frame = frame;
    }

    JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        panel.add(new JLabel("Search"), BorderLayout.WEST);
        panel.add(frame.searchField, BorderLayout.CENTER);
        return panel;
    }

    JScrollPane buildTrackPanel() {
        JScrollPane scrollPane = new JScrollPane(frame.trackList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Tracks"));
        return scrollPane;
    }

    JTabbedPane buildBrowserTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Music", buildTrackPanel());
        tabs.addTab("SFX", buildSoundEffectPanel());
        return tabs;
    }

    JPanel buildSoundEffectPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        searchPanel.add(new JLabel("Search"), BorderLayout.WEST);
        searchPanel.add(frame.soundEffectSearchField, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.NORTH);

        JScrollPane listScrollPane = new JScrollPane(frame.soundEffectList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Sound Effects"));
        panel.add(listScrollPane, BorderLayout.CENTER);

        JPanel detailsPanel = new JPanel(new BorderLayout(4, 4));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 6));
        detailsPanel.add(frame.playSoundEffectButton, BorderLayout.NORTH);
        frame.soundEffectMetadataArea.setEditable(false);
        frame.soundEffectMetadataArea.setLineWrap(true);
        frame.soundEffectMetadataArea.setWrapStyleWord(true);
        detailsPanel.add(new JScrollPane(frame.soundEffectMetadataArea), BorderLayout.CENTER);
        panel.add(detailsPanel, BorderLayout.EAST);
        return panel;
    }

    JPanel buildDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        panel.add(buildTransportPanel(), BorderLayout.NORTH);

        frame.metadataArea.setEditable(false);
        frame.metadataArea.setLineWrap(true);
        frame.metadataArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(frame.metadataArea), BorderLayout.CENTER);
        panel.add(buildChannelPanel(), BorderLayout.SOUTH);
        return panel;
    }

    JPanel buildTransportPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttons.add(frame.playButton);
        buttons.add(frame.pauseResumeButton);
        buttons.add(frame.stopButton);
        panel.add(buttons);
        panel.add(frame.loopCheckBox);
        frame.volumeSlider.setMajorTickSpacing(25);
        frame.volumeSlider.setPaintTicks(true);
        frame.volumeSlider.setPaintLabels(true);
        panel.add(new JLabel("Volume"));
        panel.add(frame.volumeSlider);
        return panel;
    }

    JPanel buildChannelPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Channels"));
        for (JLabel channelLabel : frame.channelLabels) {
            channelLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(channelLabel);
        }
        return panel;
    }

    JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        panel.add(frame.statusLabel, BorderLayout.CENTER);
        return panel;
    }
}
