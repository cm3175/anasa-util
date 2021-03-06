package net.anasa.math.interpreter.sequence;

import net.anasa.math.MathNumber;
import net.anasa.math.expression.ConstantType;
import net.anasa.math.expression.FunctionExpression;
import net.anasa.math.expression.FunctionType;
import net.anasa.math.expression.IExpression;
import net.anasa.math.expression.IFunction;
import net.anasa.math.expression.NumberExpression;
import net.anasa.math.expression.OperationExpression;
import net.anasa.math.expression.OperatorType;
import net.anasa.math.expression.VariableExpression;
import net.anasa.math.sequence.SequenceNesting;
import net.anasa.math.sequence.SequenceToken;
import net.anasa.math.sequence.SequenceToken.TokenType;
import net.anasa.util.Checks;
import net.anasa.util.Listing;
import net.anasa.util.NumberHelper;
import net.anasa.util.StringHelper;
import net.anasa.util.StringHelper.NestingException;
import net.anasa.util.resolver.ParserResolver;
import net.anasa.util.resolver.ResolverException;
import net.anasa.util.resolver.logic.ComplexResolver;
import net.anasa.util.resolver.logic.IResolver;
import net.anasa.util.resolver.logic.ParserMatchResolver;

public class ExpressionInterpreter
{
	private final ParserResolver<SequenceToken, IExpression> parser = new ParserResolver<>();
	
	public ExpressionInterpreter()
	{
		add(new ComplexResolver<SequenceToken, IExpression>("multiply")
		{
			Consumer<Listing<SequenceToken>> a = new Consumer<>(new ParserMatchResolver<>(getParser()));
			Consumer<Listing<SequenceToken>> b = new Consumer<>(new ParserMatchResolver<>(getParser()));
			
			@Override
			public IExpression resolve(ConsumerStorage storage) throws ResolverException
			{
				return getParser().resolve(new Listing<>(storage.get(a)).add(new SequenceToken(TokenType.OPERATOR, "*")).addAll(storage.get(b)));
			}
		});
		
		add(new ITypeResolver<IExpression>()
		{
			@Override
			public TokenType getType()
			{
				return TokenType.NUMBER;
			}
			
			@Override
			public IExpression resolve(SequenceToken item) throws ResolverException
			{
				String data = item.getData();
				
				ConstantType constant = ConstantType.get(data);
				if(constant != null)
				{
					return new NumberExpression(new MathNumber(constant.getValue()));
				}
				
				Checks.check(NumberHelper.isDouble(data), new ResolverException("Invalid number: " + item.getData()));
				return new NumberExpression(new MathNumber(NumberHelper.getDouble(data)));
			}
		});
		
		add(new ITypeResolver<IExpression>()
		{
			@Override
			public TokenType getType()
			{
				return TokenType.VARIABLE;
			}
			
			@Override
			public IExpression resolve(SequenceToken item) throws ResolverException
			{
				return new VariableExpression(item.getData());
			}
		});
		
		add(new IResolver<SequenceToken, IExpression>()
		{
			@Override
			public boolean matches(Listing<SequenceToken> data)
			{
				return data.size() >= 2 && data.get(0).getType() == TokenType.OPEN_PARENTHESIS && data.get(data.size() - 1).getType() == TokenType.CLOSE_PARENTHESIS;
			}
			
			@Override
			public IExpression resolve(Listing<SequenceToken> data) throws ResolverException
			{
				return getParser().resolve(data.shear(1, 1));
			}
		});
		
		add(new IResolver<SequenceToken, IExpression>()
		{
			@Override
			public boolean matches(Listing<SequenceToken> data)
			{
				try
				{
					resolve(data);
					return true;
				}
				catch(ResolverException e)
				{
					return false;
				}
			}
			
			@Override
			public IExpression resolve(Listing<SequenceToken> data) throws ResolverException
			{
				try
				{
					Checks.check(SequenceNesting.isNestingValid(data) && SequenceNesting.stripNesting(data).contains((item) -> item.getType() == TokenType.OPERATOR),
							new ResolverException("Invalid operator input data: " + data));
					
					SequenceToken splitter = null;
					
					for(SequenceToken item : SequenceNesting.stripNesting(data))
					{
						if(item.getType() == TokenType.OPERATOR)
						{
							OperatorType op = OperatorType.get(item.getData());
							
							if(splitter == null || !op.hasPriority(OperatorType.get(splitter.getData())))
							{
								splitter = item;
							}
						}
					}
					
					Checks.checkNotNull(splitter, new ResolverException("Sequence must contain an operator"));
					
					int index = data.indexOf(splitter);
					
					IExpression a = getParser().resolve(data.subList(0, index));
					IExpression b = getParser().resolve(data.subList(index + 1));
					
					return new OperationExpression(OperatorType.get(splitter.getData()), a, b);
				}
				catch(NestingException e)
				{
					throw new ResolverException(e);
				}
			}
		});
		
		add(new ComplexResolver<SequenceToken, IExpression>("function")
		{
			Consumer<IFunction> function = new Consumer<>(new ITypeResolver<IFunction>()
			{
				@Override
				public TokenType getType()
				{
					return TokenType.FUNCTION;
				}
				
				@Override
				public IFunction resolve(SequenceToken item) throws ResolverException
				{
					return FunctionType.get(item.getData());
				}
			});
			
			Consumer<IExpression> operand = new Consumer<>(getParser());
			
			@Override
			public IExpression resolve(ConsumerStorage storage) throws ResolverException
			{
				return new FunctionExpression(storage.get(function), storage.get(operand));
			}
		});
		
		add(new ComplexResolver<SequenceToken, IExpression>("negative")
		{
			Consumer<OperatorType> negative = new Consumer<>(new ITypeResolver<OperatorType>()
			{
				@Override
				public TokenType getType()
				{
					return TokenType.OPERATOR;
				}
				
				@Override
				public boolean matches(SequenceToken item)
				{
					return StringHelper.equals(OperatorType.SUBTRACT.getSignature(), item.getData());
				}
				
				@Override
				public OperatorType resolve(SequenceToken item) throws ResolverException
				{
					return OperatorType.SUBTRACT;
				}
			});
			
			Consumer<IExpression> operand = new Consumer<>(getParser());
			
			@Override
			public IExpression resolve(ConsumerStorage storage) throws ResolverException
			{
				return new OperationExpression(storage.get(negative), new NumberExpression(new MathNumber(0)), storage.get(operand));
			}
		});
	}
	
	private <T extends IResolver<SequenceToken, IExpression>> T add(T resolver)
	{
		getParser().add(resolver);
		
		return resolver;
	}
	
	public ParserResolver<SequenceToken, IExpression> getParser()
	{
		return parser;
	}
	
	public IExpression getFrom(Listing<SequenceToken> data) throws ResolverException
	{
		return getParser().resolve(data);
	}
}
