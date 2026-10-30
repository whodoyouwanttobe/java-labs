package org.example.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import org.example.store.TrieNodeView;

import java.util.Locale;

public class TriePane extends ScrollPane {

    private static final double STEP_X = 30;
    private static final double STEP_Y = 26;
    private static final double RADIUS = 10;
    private static final double MARGIN = 20;
    private static final double MIN_WIDTH = 400;
    private static final double MIN_HEIGHT = 300;

    private final Canvas canvas = new Canvas(MIN_WIDTH, MIN_HEIGHT);
    private int row;

    public TriePane() {
        setContent(canvas);
        setPannable(true);
    }

    public void draw(TrieNodeView root, String prefix, String highlightKey) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (root == null) {
            g.setFill(Color.BLACK);
            g.fillText("Нет имён, начинающихся на \"" + prefix + "\"", MARGIN, MARGIN);
            return;
        }
        canvas.setWidth(Math.max(MIN_WIDTH, MARGIN * 2 + depth(root) * STEP_X + 40));
        canvas.setHeight(Math.max(MIN_HEIGHT, MARGIN * 2 + leaves(root) * STEP_Y));
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        String key = highlightKey == null ? "" : highlightKey.trim().toLowerCase(Locale.ROOT);
        row = 0;
        drawNode(g, root, 0, prefix.trim().toLowerCase(Locale.ROOT), key);
    }

    private double drawNode(GraphicsContext g, TrieNodeView node, int depth, String path, String key) {
        double x = MARGIN + depth * STEP_X;
        TrieNodeView[] children = node.children();
        int shown = shownChildren(node);
        double[] childY = new double[shown];
        for (int i = 0; i < shown; i++) {
            childY[i] = drawNode(g, children[i], depth + 1, path + children[i].symbol(), key);
        }

        double y;
        if (shown == 0) {
            y = MARGIN + row * STEP_Y;
            row++;
        } else {
            y = (childY[0] + childY[shown - 1]) / 2;
        }

        for (int i = 0; i < shown; i++) {
            boolean onPath = isOnPath(key, path + children[i].symbol());
            g.setStroke(onPath ? Color.RED : Color.GRAY);
            g.strokeLine(x + RADIUS, y, x + STEP_X - RADIUS, childY[i]);
        }

        g.setFill(node.hasValue() ? Color.LIGHTGREEN : Color.WHITE);
        g.fillOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
        g.setStroke(isOnPath(key, path) ? Color.RED : Color.BLACK);
        g.strokeOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);

        g.setFill(Color.BLACK);
        if (depth == 0) {
            String title = path.isEmpty() ? "корень" : path;
            g.fillText(title + " (" + node.count() + ")", x - RADIUS, y + RADIUS + 12);
        } else {
            g.fillText(String.valueOf(node.symbol()), x - 4, y + 4);
        }
        if (shown < children.length) {
            g.fillText("...", x + RADIUS + 3, y + 4);
        }
        return y;
    }

    private int shownChildren(TrieNodeView node) {
        int shown = 0;
        while (shown < node.children().length && node.children()[shown] != null) {
            shown++;
        }
        return shown;
    }

    private boolean isOnPath(String key, String path) {
        return !key.isEmpty() && key.startsWith(path);
    }

    private int depth(TrieNodeView node) {
        int max = 0;
        for (int i = 0; i < shownChildren(node); i++) {
            max = Math.max(max, depth(node.children()[i]) + 1);
        }
        return max;
    }

    private int leaves(TrieNodeView node) {
        int shown = shownChildren(node);
        if (shown == 0) {
            return 1;
        }
        int sum = 0;
        for (int i = 0; i < shown; i++) {
            sum += leaves(node.children()[i]);
        }
        return sum;
    }
}
