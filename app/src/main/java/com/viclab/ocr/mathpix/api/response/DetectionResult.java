package com.viclab.ocr.mathpix.api.response;

import java.util.ArrayList;

public class DetectionResult {
    public DetectionMap detection_map;
    public String error;
    public String latex;
    public ArrayList<String> latex_list;
    public double latex_confidence;
    public Position position;

    public static class DetectionMap {
        public double contains_chat;
        public double contains_diagram;
        public double contains_geometry;
        public double contains_graph;
        public double contains_table;

        public double is_inverted;
        public double is_not_math;
        public double is_printed;
    }

    public static class Position {
        public double width;
        public double height;
        public double top_left_x;
        public double top_left_y;
    }
}
