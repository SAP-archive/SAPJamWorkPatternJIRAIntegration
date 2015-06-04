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
public class FilterFilter implements ExpressionVisitor {

  private final List<String> nameMatchValues;

  public FilterFilter() {
    this.nameMatchValues = new ArrayList<String>();
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
    return leftSide + jqlOperator + rightSide;
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
      if (((String) args.get(0)).equals("Name")) {
        if (value.startsWith("'") && value.endsWith("'")) {
          nameMatchValues.add(value.substring(1, value.length() - 1).toLowerCase());
        } else {
          nameMatchValues.add(value);
        }
        return args.get(0) + "=" + value;
      }
    } else if ("SUBSTRINGOF".equals(operator.name())) {
      String value = (String) args.get(0);
      if (((String) args.get(1)).equals("Name")) {
        if (value.startsWith("'") && value.endsWith("'")) {
          nameMatchValues.add(value.substring(1, value.length() - 1).toLowerCase());
        } else {
          nameMatchValues.add(value);
        }
        return args.get(1) + "=" + value;
      }
    }

    return null;
  }

  @Override
  public Object visitMember(MemberExpression arg0, Object arg1, Object arg2
  ) {
    return null;
  }

  @Override
  public Object visitOrder(OrderExpression arg0, Object arg1, SortOrder arg2
  ) {
    return null;
  }

  @Override
  public Object visitOrderByExpression(OrderByExpression arg0, String arg1,
          List<Object> arg2
  ) {
    return null;
  }

  @Override
  public Object visitUnary(UnaryExpression arg0, UnaryOperator arg1,
          Object arg2
  ) {
    return null;
  }

  // returns true if the value is selected by the filter
  public boolean postFilter(String value) {
    if (nameMatchValues.isEmpty()) {                    
      return true;
    } else {
      for (String partialKey : nameMatchValues) {
        if (value.contains(partialKey)) {
          return true;
        }
      }                    
      return false;
    }
  }
}
