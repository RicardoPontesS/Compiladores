package sintatica;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    private String value;
    private List<TreeNode> children;

    public TreeNode(String value) {
        this.value = value;
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode child) {
        this.children.add(child);
    }

    public String getValue() {
        return value;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return print("", true);
    }

    private String print(String prefix, boolean isTail) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix).append(isTail ? "└── " : "├── ").append(value).append("\n");
        for (int i = 0; i < children.size() - 1; i++) {
            builder.append(children.get(i).print(prefix + (isTail ? "    " : "│   "), false));
        }
        if (children.size() > 0) {
            builder.append(children.get(children.size() - 1).print(prefix + (isTail ? "    " : "│   "), true));
        }
        return builder.toString();
    }
}
