package org.example.engine.mapping;

import java.util.regex.Pattern;

public class AbstractMapping implements Comparable<AbstractMapping> {

    final Pattern pattern;
    final String url;

    public AbstractMapping(String urlPattern) {
        this.pattern = buildPattern(urlPattern);
        this.url = urlPattern;
    }

    @Override
    public int compareTo(AbstractMapping o) {
        int cmp = this.priority() - o.priority();
        if (cmp == 0) {
            cmp = this.url.compareTo(o.url);
        }
        return cmp;
    }

    int priority() {
        if ("/".equals(this.url)) {
            return Integer.MAX_VALUE;
        }
        if (this.url.startsWith("*")) {
            return Integer.MAX_VALUE - 1;
        }
        return 10000 - this.url.length();
    }


    Pattern buildPattern(String urlPattern) {
        StringBuilder sb = new StringBuilder(urlPattern.length() + 16);
        sb.append('^');
        for (int i = 0; i < urlPattern.length(); i++) {
            char ch = urlPattern.charAt(i);
            if (ch == '*') {
                sb.append(".*");
            } else if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') {
                sb.append(ch);
            } else {
                sb.append('\\').append(ch);
            }
        }
        sb.append('$');
        return Pattern.compile(sb.toString());
    }

    public boolean matcher(String uri) {
        return pattern.matcher(uri).matches();
    }
}
