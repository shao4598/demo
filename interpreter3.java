public interface Expression {
  boolean interpret(Map<String, Long> stats);
}

// >>
public class GreaterExpression implements Expression {
  private String key;
  private long value;

  // "api_error_per_minute > 100"
  // "api_count_per_minute > 10000"
  public GreaterExpression(String strExpression) {
    String[] elements = strExpression.trim().split("\\s+");
    if (elements.length != 3 || !elements[1].trim().equals(">")) {
      throw new RuntimeException("Expression is invalid: " + strExpression);
    }
    this.key = elements[0].trim();
    this.value = Long.parseLong(elements[2].trim());
  }

  public GreaterExpression(String key, long value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public boolean interpret(Map<String, Long> stats) {
    if (!stats.containsKey(key)) {
      return false;
    }
    long statValue = stats.get(key);
    return statValue > value;
  }
}

// <<
public class LessExpression implements Expression {}

// ==
public class EqualExpression implements Expression {}

// &&
public class AndExpression implements Expression {
  private List<Expression> expressions = new ArrayList<>();

  public AndExpression(String strAndExpression) {
    // "api_error_per_minute > 100"
    // "api_count_per_minute > 10000"
    String[] strExpressions = strAndExpression.split("&&");
    // ["api_error_per_minute > 100"]
    // ["api_count_per_minute > 10000"]
    for (String strExpr : strExpressions) {
      if (strExpr.contains(">")) {
        expressions.add(new GreaterExpression(strExpr));
      } else if (strExpr.contains("<")) {
        expressions.add(new LessExpression(strExpr));
      } else if (strExpr.contains("==")) {
        expressions.add(new EqualExpression(strExpr));
      } else {
        throw new RuntimeException("Expression is invalid: " + strAndExpression);
      }
    }
  }

  public AndExpression(List<Expression> expressions) {
    this.expressions.addAll(expressions);
  }

  @Override
  public boolean interpret(Map<String, Long> stats) {
    for (Expression expr : expressions) {
      if (!expr.interpret(stats)) {
        return false;
      }
    }
    return true;
  }

}

// ||
public class OrExpression implements Expression {
  private List<Expression> expressions = new ArrayList<>();

  public OrExpression(String strOrExpression) {
    // ["api_error_per_minute > 100", "api_count_per_minute > 10000"]
    String[] andExpressions = strOrExpression.split("\\|\\|");
    for (String andExpr : andExpressions) {
      expressions.add(new AndExpression(andExpr));
    }
  }

  public OrExpression(List<Expression> expressions) {
    this.expressions.addAll(expressions);
  }

  @Override
  public boolean interpret(Map<String, Long> stats) {
    for (Expression expr : expressions) {
      if (expr.interpret(stats)) {
        return true;
      }
    }
    return false;
  }
}

public class AlertRuleInterpreter {
  private Expression expression;

  public AlertRuleInterpreter(String ruleExpression) {
    // "api_error_per_minute > 100 || api_count_per_minute > 10000"
    this.expression = new OrExpression(ruleExpression);
  }

  public boolean interpret(Map<String, Long> stats) {
    return expression.interpret(stats);
  }
} 