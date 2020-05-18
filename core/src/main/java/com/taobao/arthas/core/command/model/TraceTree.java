package com.taobao.arthas.core.command.model;


import com.taobao.arthas.core.util.ClassUtils;

import java.util.List;
import java.util.Map;

/**
 * @author gongdewei 2020/4/28
 */
public class TraceTree {
    private TraceNode root;

    private TraceNode current;
    private Map<String, String> normalizeClassNameMap;
    private int nodeCount = 0;

    public TraceTree(ThreadNode root, Map<String, String> normalizeClassNameMap) {
        this.root = root;
        this.current = root;
        this.normalizeClassNameMap = normalizeClassNameMap;
    }

    public void begin(String className, String methodName) {
        //non-invoking
        begin(className, methodName, -1, false);
    }

    public void begin(String className, String methodName, int lineNumber) {
        //invoking
        begin(className, methodName, lineNumber, true);
    }

    private void begin(String className, String methodName, int lineNumber, boolean isInvoking) {
        //Integer nodeId = getNodeId(className, methodName, lineNumber);
        TraceNode child = findChild(current, className, methodName, lineNumber);
        if (child == null) {
            child = new MethodNode(className, methodName, lineNumber, isInvoking);
            current.addChild(child);
        }
        child.begin();
        current = child;
        nodeCount += 1;
    }

    private TraceNode findChild(TraceNode node, String className, String methodName, int lineNumber) {
        List<TraceNode> childList = node.getChildren();
        if (childList != null) {
            //less memory than foreach/iterator
            for (int i = 0; i < childList.size(); i++) {
                TraceNode child = childList.get(i);
                if (matchNode(child, className, methodName, lineNumber)) {
                    return child;
                }
            }
        }
        return null;
    }

    private boolean matchNode(TraceNode node, String className, String methodName, int lineNumber) {
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            if (lineNumber != methodNode.getLineNumber()) return false;
            if (className != null ? !className.equals(methodNode.getClassName()) : methodNode.getClassName() != null) return false;
            return methodName != null ? methodName.equals(methodNode.getMethodName()) : methodNode.getMethodName() == null;
        }
        return false;
    }

    public void end() {
        current.end();
        if (current.parent() != null) {
            //TODO 为什么会到达这里？ 调用end次数比begin多？
            current = current.parent();
        }
    }

    public void end(String exceptionClassName, int lineNumber) {
        if (current instanceof MethodNode) {
            MethodNode currentNode = (MethodNode) current;
            currentNode.setThrow(true);
            currentNode.setThrowExp(exceptionClassName);
            currentNode.setLineNumber(lineNumber);
        }
        current.setMark("throw:"+exceptionClassName);
        this.end();
    }

    public void end(boolean isThrow) {
        if (isThrow) {
            current.setMark("throws Exception");
            if (current instanceof MethodNode) {
                MethodNode methodNode = (MethodNode) current;
                methodNode.setThrow(true);
            }
        }
        this.end();
    }

//    private int getNodeId(String className, String methodName, int lineNumber) {
//        //from Arrays.hashCode(Object a[])
//        //memory optimizing: avoid create new object[]
//        int result = 1;
//        result = 31 * result + className.hashCode();
//        result = 31 * result + methodName.hashCode();
//        result = 31 * result + lineNumber;
//        return result;
//    }

    /**
     * 转换标准类名，放在trace结束后统一转换，减少重复操作
     * @param node
     */
    public void normalizeClassName(TraceNode node) {
        if (node instanceof MethodNode) {
            MethodNode methodNode = (MethodNode) node;
            String nodeClassName = methodNode.getClassName();
            String normalizeClassName = ClassUtils.normalizeClassName(nodeClassName, normalizeClassNameMap);
            methodNode.setClassName(normalizeClassName);
        }
        List<TraceNode> children = node.getChildren();
        if (children != null) {
            //less memory fragment than foreach
            for (int i = 0; i < children.size(); i++) {
                TraceNode child = children.get(i);
                normalizeClassName(child);
            }
        }
    }

    public TraceNode getRoot() {
        return root;
    }

    public TraceNode current() {
        return current;
    }

    public int getNodeCount() {
        return nodeCount;
    }
}
