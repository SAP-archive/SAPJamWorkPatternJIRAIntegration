package com.sap.jam.samples.jira.plugin.odata.server.filters;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmLiteral;
import org.apache.olingo.odata2.api.edm.EdmTyped;
import org.apache.olingo.odata2.api.uri.expression.BinaryExpression;
import org.apache.olingo.odata2.api.uri.expression.BinaryOperator;
import org.apache.olingo.odata2.api.uri.expression.ExpressionVisitor;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.LiteralExpression;
import org.apache.olingo.odata2.api.uri.expression.MemberExpression;
import org.apache.olingo.odata2.api.uri.expression.MethodExpression;
import org.apache.olingo.odata2.api.uri.expression.MethodOperator;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderExpression;
import org.apache.olingo.odata2.api.uri.expression.PropertyExpression;
import org.apache.olingo.odata2.api.uri.expression.SortOrder;
import org.apache.olingo.odata2.api.uri.expression.UnaryExpression;
import org.apache.olingo.odata2.api.uri.expression.UnaryOperator;

// Converts an OData filter into a JIRA filter
public class IssueFilter implements ExpressionVisitor {

  private final List<String> keyMatchValues;

  public IssueFilter() {
    this.keyMatchValues = new ArrayList<String>();
  }

  @Override
  public Object visitFilterExpression(FilterExpression filterExpression, String expressionString, Object expression) {
    return expression;
  }

  @Override
  public Object visitBinary(BinaryExpression binaryExpression, BinaryOperator operator, Object leftSide, Object rightSide) {
    String jqlOperator = "";
    switch (operator) {
      case EQ:
        jqlOperator = "=";
        break;
      case NE:
        jqlOperator = "<>";
        break;
      case OR:
        jqlOperator = " or ";
        break;
      case AND:
        jqlOperator = " and ";
        break;
      case GE:
        jqlOperator = ">=";
        break;
      case GT:
        jqlOperator = ">";
        break;
      case LE:
        jqlOperator = "<=";
        break;
      case LT:
        jqlOperator = "<";
        break;
      default:
        break;
    }
    return "(" + leftSide + jqlOperator + rightSide + ")";
  }

  @Override
  public Object visitLiteral(LiteralExpression literal, EdmLiteral edmLiteral) {
    return "\'" + edmLiteral.getLiteral() + "\'";
  }

  @Override
  public Object visitProperty(PropertyExpression propertyExpression, String uriLiteral, EdmTyped edmProperty) {
    try {
      return edmProperty.getName();
    } catch (EdmException e) {
      return null;
    }
  }

  @Override
  public Object visitMethod(MethodExpression expression, MethodOperator operator, List<Object> args) {
    // We will only implement the 'startswith' and 'substringof' operator, 
    // which are used by the Jam object search field, but we will implement it 
    // as a substring search
    if ("STARTSWITH".equals(operator.name())) {
      String value = (String) args.get(1);
      if (((String) args.get(0)).equals("Key")) {
        if (value.startsWith("'") && value.endsWith("'")) {
          keyMatchValues.add(value.substring(1, value.length() - 1).toLowerCase());
        } else {
          keyMatchValues.add(value);
        }
        return "(" + args.get(0) + "=" + value + " OR issue in watchedIssues())";
      } else {
        return args.get(0) + "~" + value;
      }
    } else if ("SUBSTRINGOF".equals(operator.name())) {
      String value = (String) args.get(0);
      if (((String) args.get(1)).equals("Key")) {
        if (value.startsWith("'") && value.endsWith("'")) {
          keyMatchValues.add(value.substring(1, value.length() - 1).toLowerCase());
        } else {
          keyMatchValues.add(value);
        }
        return "(" + args.get(1) + "=" + value + " OR issue in watchedIssues())";
      } else {
        return args.get(1) + "~" + value;
      }
    }

    return null;
  }

  @Override
  public Object visitMember(MemberExpression arg0, Object arg1, Object arg2) {
    return null;
  }

  @Override
  public Object visitOrder(OrderExpression orderByExpression, Object filterResult, SortOrder sortOrder) {
    return filterResult + " " + sortOrder.toString();
  }

  @Override
  public Object visitOrderByExpression(OrderByExpression orderByExpression, String expressionString, List<Object> orders) {
    StringBuilder buffer = new StringBuilder();
    
    for(Object order : orders) {
      if (buffer.length() > 0) {
        buffer.append(",");
      }
      buffer.append(order);
    }
    
    if (buffer.length() > 0) {
      buffer.insert(0, " order by ");
    }
    
    return buffer.toString();
  }

  @Override
  public Object visitUnary(UnaryExpression arg0, UnaryOperator arg1, Object arg2) {
    return null;
  }

  // returns true if the value is selected by the filter
  public boolean postFilter(String value) {
    if (keyMatchValues.isEmpty()) {                    
      return true;
    } else {
      for (String partialKey : keyMatchValues) {
        if (value.contains(partialKey)) {
          return true;
        }
      }                    
      return false;
    }
  }
}
