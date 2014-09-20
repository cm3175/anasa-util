package net.anasa.math.expression;

import net.anasa.math.MathData;
import net.anasa.math.MathException;
import net.anasa.math.MathNumber;

public class OperationExpression extends MathExpression
{
	private final OperatorType operator;
	private final IExpression a, b;
	
	public OperationExpression(OperatorType operator, IExpression a, IExpression b)
	{
		this.operator = operator;
		
		this.a = a;
		this.b = b;
	}

	public OperatorType getOperator()
	{
		return operator;
	}

	public IExpression getA()
	{
		return a;
	}

	public IExpression getB()
	{
		return b;
	}

	@Override
	public MathNumber evaluate(MathData data) throws MathException
	{
		return getOperator().getResult(getA().evaluate(data), getB().evaluate(data), data);
	}

	@Override
	public String getStringValue()
	{
		return getDisplayData(getA()) + getOperator() + getDisplayData(getB());
	}
	
	private String getDisplayData(IExpression expression)
	{
		if(expression instanceof OperationExpression && getOperator().hasPriority(((OperationExpression)expression).getOperator()))
		{
			return "(" + expression + ")";
		}
		
		return expression.toString();
	}
}
