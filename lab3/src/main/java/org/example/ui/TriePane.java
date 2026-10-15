package org.example.ui;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.example.store.TrieNodeView;

class TriePane extends ScrollPane {

    private static final double COLUMN_WIDTH = 34;
    private static final double ROW_HEIGHT = 34;
    private static final double RADIUS = 11;
    private static final double MARGIN = 40;
    private static final double MIN_WIDTH = 400;
    private static final double MIN_HEIGHT = 300;

    private static final Color BACKGROUND = Color.WHITE;
    private static final Color EDGE = Color.web("#b0bec5");
    private static final Color NODE_FILL = Color.WHITE;
    private static final Color NODE_STROKE = Color.web("#546e7a");
    private static final Color TERMINAL_FILL = Color.web("#a5d6a7");
    private static final Color VISITED = Color.web("#ef6c00");
    private static final Color VISITED_FILL = Color.web("#ffe0b2");
    private static final Color TEXT = Color.web("#263238");
    private static final Color HINT = Color.web("#90a4ae");

    private static final Font LETTER_FONT = Font.font("Monospaced", 13);
    private static final Font SMALL_FONT = Font.font("System", 10);

    private final Canvas canvas = new Canvas(MIN_WIDTH, MIN_HEIGHT);
    private int rows;
    private int maxDepth;

    private static final class Placed {
        final TrieNodeView view;
        final Placed[] children;
        double x;
        double y;

        Placed(TrieNodeView view) {
            this.view = view;
            this.children = new Placed[view.children().length];
        }
    }

    TriePane() {
        setContent(canvas);
        setPannable(true);
    }

    void show(TrieNodeView root, String prefix) {
        if (root == null) {
            resizeCanvas(MIN_WIDTH, MIN_HEIGHT);
            message("Нет ни одного имени с префиксом «" + prefix + "»");
            return;
        }
        rows = 0;
        maxDepth = 0;
        Placed tree = place(root, 0);
        resizeCanvas(MARGIN * 2 + maxDepth * COLUMN_WIDTH + 80, MARGIN * 2 + Math.max(rows - 1, 0) * ROW_HEIGHT + 20);

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawEdges(g, tree);
        drawNodes(g, tree, true);
        scrollToRoot(tree);
    }

    private void scrollToRoot(Placed root) {
        setHvalue(0);
        double viewport = getViewportBounds().getHeight();
        double extra = canvas.getHeight() - viewport;
        double value = extra <= 0 ? 0 : (root.y - viewport / 2) / extra;
        setVvalue(Math.max(0, Math.min(1, value)));
    }

    private Placed place(TrieNodeView view, int depth) {
        Placed p = new Placed(view);
        p.x = MARGIN + depth * COLUMN_WIDTH;
        maxDepth = Math.max(maxDepth, depth);
        TrieNodeView[] kids = view.children();
        if (kids.length == 0) {
            p.y = MARGIN + rows * ROW_HEIGHT;
            rows++;
            return p;
        }
        for (int i = 0; i < kids.length; i++) {
            p.children[i] = place(kids[i], depth + 1);
        }
        p.y = (p.children[0].y + p.children[kids.length - 1].y) / 2;
        return p;
    }

    private void drawEdges(GraphicsContext g, Placed p) {
        for (Placed child : p.children) {
            boolean onPath = p.view.visited() && child.view.visited();
            g.setStroke(onPath ? VISITED : EDGE);
            g.setLineWidth(onPath ? 2.5 : 1);
            g.strokeLine(p.x, p.y, child.x, child.y);
            drawEdges(g, child);
        }
    }

    private void drawNodes(GraphicsContext g, Placed p, boolean isRoot) {
        TrieNodeView v = p.view;
        Color fill = v.terminal() ? TERMINAL_FILL : (v.visited() ? VISITED_FILL : NODE_FILL);
        g.setFill(fill);
        g.fillOval(p.x - RADIUS, p.y - RADIUS, RADIUS * 2, RADIUS * 2);
        g.setStroke(v.visited() ? VISITED : NODE_STROKE);
        g.setLineWidth(v.visited() ? 3 : 1);
        g.strokeOval(p.x - RADIUS, p.y - RADIUS, RADIUS * 2, RADIUS * 2);

        g.setFill(TEXT);
        g.setFont(LETTER_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(isRoot ? "•" : letter(v.symbol()), p.x, p.y);

        g.setFont(SMALL_FONT);
        g.setFill(HINT);
        if (isRoot) {
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText(v.isRoot() ? "root (" + v.subtreeSize() + ")" : "«" + v.prefix() + "» (" + v.subtreeSize() + ")",
                    p.x - RADIUS, p.y + RADIUS + 10);
        } else if (p.children.length != 1) {
            g.fillText(String.valueOf(v.subtreeSize()), p.x, p.y - RADIUS - 6);
        }
        if (v.truncated()) {
            g.setTextAlign(TextAlignment.LEFT);
            g.fillText("…", p.x + RADIUS + 3, p.y);
        }

        for (Placed child : p.children) {
            drawNodes(g, child, false);
        }
    }

    private void message(String text) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(BACKGROUND);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setFill(HINT);
        g.setFont(Font.font("System", 14));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.fillText(text, 20, 20);
    }

    private void resizeCanvas(double width, double height) {
        canvas.setWidth(Math.max(MIN_WIDTH, width));
        canvas.setHeight(Math.max(MIN_HEIGHT, height));
    }

    private static String letter(char c) {
        return c == ' ' ? "␣" : String.valueOf(c);
    }
}
