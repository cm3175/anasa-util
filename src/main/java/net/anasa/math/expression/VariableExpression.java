package net.anasa.math.expression;

import net.anasa.math.MathData;
import net.anasa.math.MathException;
import net.anasa.math.MathNumber;
import net.anasa.util.Checks;

public class VariableExpression extends MathExpression implements IVariable
{
	private final String name;
	
	public VariableExpression(String name)
	{
		this.name = name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public MathNumber evaluate(MathData data) throws MathException
	{
		Checks.check(data.hasVariable(getName()), new MathException("Variable is not defined: " + getName()));
		return data.getVariable(getName());
	}

	@Override
	public String getStringValue()
	{
		return getName();
	}
	
	@Override
	public IExpression[] getChildren()
	{
		return new IExpression[] {};
	}
}
