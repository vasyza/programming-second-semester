package org.example.client.gui;

import org.example.client.i18n.LocaleManager;
import org.example.common.model.Worker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VisualizationPanel extends JPanel {
    private List<Worker> workers = new ArrayList<>();
    private final Map<Integer, Color> userColors = new HashMap<>();
    private final Color[] palette = {
            new Color(230, 25, 75), new Color(60, 180, 75), new Color(255, 225, 25),
            new Color(0, 130, 200), new Color(245, 130, 48), new Color(145, 30, 180),
            new Color(70, 240, 240), new Color(240, 50, 230), new Color(210, 245, 60)
    };
    private final Map<Long, Animation> animationState = new HashMap<>();
    private int currentUserId;

    private static class Animation {
        float currentSize;
        float targetSize;
        double currentX, currentY;
        double targetX, targetY;
    }

    public VisualizationPanel() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = workers.size() - 1; i >= 0; i--) {
                    Worker worker = workers.get(i);
                    Animation anim = animationState.get(worker.getId());
                    if (anim == null) continue;
                    if (e.getPoint().distance(anim.currentX, anim.currentY) <= anim.currentSize / 2.0) {
                        showObjectInfo(worker);
                        return;
                    }
                }
            }
        });
        Timer animationTimer = new Timer(20, e -> animate());
        animationTimer.start();
    }

    public void setWorkers(List<Worker> newWorkers, int currentUserId) {
        this.currentUserId = currentUserId;
        this.workers = new ArrayList<>(newWorkers);

        float minX = 0, maxX = 0;
        double minY = 0, maxY = 0;
        if (!workers.isEmpty()) {
            minX = workers.get(0).getCoordinates().getX();
            maxX = minX;
            minY = workers.get(0).getCoordinates().getY();
            maxY = minY;
            for (Worker w : workers) {
                if (w.getCoordinates().getX() < minX) minX = w.getCoordinates().getX();
                if (w.getCoordinates().getX() > maxX) maxX = w.getCoordinates().getX();
                if (w.getCoordinates().getY() < minY) minY = w.getCoordinates().getY();
                if (w.getCoordinates().getY() > maxY) maxY = w.getCoordinates().getY();
            }
        }
        float xRange = (maxX - minX == 0) ? 1 : maxX - minX;
        double yRange = (maxY - minY == 0) ? 1 : maxY - minY;
        int padding = 50;
        int panelWidth = Math.max(1, getWidth() - 2 * padding);
        int panelHeight = Math.max(1, getHeight() - 2 * padding);

        Map<Long, Animation> newAnimationState = new HashMap<>();

        for (Worker worker : this.workers) {
            userColors.computeIfAbsent(worker.getOwnerId(), id -> palette[userColors.size() % palette.length]);

            double targetX = padding + ((worker.getCoordinates().getX() - minX) / xRange * panelWidth);
            double targetY = padding + ((worker.getCoordinates().getY() - minY) / yRange * panelHeight);
            float targetSize = getWorkerTargetSize(worker);

            if (animationState.containsKey(worker.getId())) {
                Animation existingAnim = animationState.get(worker.getId());
                existingAnim.targetX = targetX;
                existingAnim.targetY = targetY;
                existingAnim.targetSize = targetSize;
                newAnimationState.put(worker.getId(), existingAnim);
            } else {
                Animation newAnim = new Animation();
                newAnim.currentSize = 0;
                newAnim.targetSize = targetSize;
                newAnim.currentX = targetX;
                newAnim.currentY = targetY;
                newAnim.targetX = targetX;
                newAnim.targetY = targetY;
                newAnimationState.put(worker.getId(), newAnim);
            }
        }

        this.animationState.clear();
        this.animationState.putAll(newAnimationState);
    }

    private float getWorkerTargetSize(Worker worker) {
        float baseSize = 20;
        float salaryBonus = worker.getSalary() != null ? worker.getSalary() / 5000f : 0;
        return Math.min(baseSize + salaryBonus, 80);
    }

    private void animate() {
        boolean needsRepaint = false;
        if (getWidth() == 0 || getHeight() == 0) return;

        for (Animation anim : animationState.values()) {
            if (Math.abs(anim.currentSize - anim.targetSize) > 0.1f) {
                anim.currentSize += (anim.targetSize - anim.currentSize) * 0.1f;
                needsRepaint = true;
            }
            if (Math.abs(anim.currentX - anim.targetX) > 0.1f || Math.abs(anim.currentY - anim.targetY) > 0.1f) {
                anim.currentX += (anim.targetX - anim.currentX) * 0.1f;
                anim.currentY += (anim.targetY - anim.currentY) * 0.1f;
                needsRepaint = true;
            }
        }
        if (needsRepaint) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (getWidth() == 0 || getHeight() == 0) return;

        for (Worker worker : workers) {
            Animation anim = animationState.get(worker.getId());
            if (anim == null) continue;

            int size = (int) anim.currentSize;
            if (size < 2) continue;

            int x = (int) anim.currentX;
            int y = (int) anim.currentY;

            g2d.setColor(userColors.get(worker.getOwnerId()));
            g2d.fillOval(x - size / 2, y - size / 2, size, size);

            if (worker.getOwnerId() == currentUserId) {
                g2d.setColor(Color.BLACK);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x - size / 2, y - size / 2, size, size);
            }
        }
    }

    private void showObjectInfo(Worker worker) {
        String info = LocaleManager.getFormattedString("object.info.text",
                worker.getId(), worker.getName(), worker.getOwnerId(),
                worker.getSalary() != null ? worker.getSalary() : "N/A");
        JOptionPane.showMessageDialog(this, info,
                LocaleManager.getString("object.info.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }
}