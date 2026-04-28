package linksawakening.dialog;

public final class DialogController {

    private final int lineWidth;
    private String text = "";
    private int visibleCharacters;
    private boolean active;

    public DialogController(int lineWidth) {
        if (lineWidth <= 0) {
            throw new IllegalArgumentException("lineWidth must be positive");
        }
        this.lineWidth = lineWidth;
    }

    public void open(String text) {
        this.text = text == null ? "" : text;
        visibleCharacters = 0;
        active = true;
    }

    public void tick() {
        if (!active || visibleCharacters >= text.length()) {
            return;
        }
        visibleCharacters++;
    }

    public void advance() {
        if (!active) {
            return;
        }
        if (visibleCharacters < text.length()) {
            visibleCharacters = text.length();
        } else {
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public String visibleText() {
        return firstTwoLines(wrap(text.substring(0, Math.min(visibleCharacters, text.length()))));
    }

    private String wrap(String value) {
        StringBuilder wrapped = new StringBuilder();
        int lineLength = 0;
        for (String word : value.split(" ", -1)) {
            if (word.isEmpty()) {
                if (lineLength < lineWidth) {
                    appendSpace(wrapped);
                    lineLength++;
                }
                continue;
            }
            if (lineLength > 0 && lineLength + 1 + word.length() > lineWidth) {
                wrapped.append('\n');
                lineLength = 0;
            } else if (lineLength > 0) {
                appendSpace(wrapped);
                lineLength++;
            }
            wrapped.append(word);
            lineLength += word.length();
        }
        return wrapped.toString();
    }

    private static String firstTwoLines(String value) {
        int firstBreak = value.indexOf('\n');
        if (firstBreak < 0) {
            return value;
        }
        int secondBreak = value.indexOf('\n', firstBreak + 1);
        if (secondBreak < 0) {
            return value;
        }
        return value.substring(0, secondBreak);
    }

    private static void appendSpace(StringBuilder builder) {
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != '\n') {
            builder.append(' ');
        }
    }
}
