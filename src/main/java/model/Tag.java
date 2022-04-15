package model;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Tag {

    private String label;
    private Rectangle2D rect;

    public Tag(String label, Rectangle2D rect) {
        this.rect = rect;
        this.label = label;
    }


}
