package net.anasa.util.resolver.logic;

import net.anasa.util.Debug;
import net.anasa.util.Listing;
import net.anasa.util.Mapping;
import net.anasa.util.resolver.ResolverCache;
import net.anasa.util.resolver.ResolverCache.ICacheEntry;
import net.anasa.util.resolver.ResolverException;

public abstract class ComplexResolver<I extends ICacheEntry<I>, T> implements IResolver<I, T>
{
	private static boolean DEBUG = false;
	
	private final String id;
	
	private final Listing<Consumer<?>> consumers = new Listing<>();
	
	private final ResolverCache<I, T> cache = new ResolverCache<>();
	
	public ComplexResolver(String id)
	{
		this.id = id;
		
		init();
	}
	
	public void init()
	{
		
	}
	
	public String getID()
	{
		return id;
	}
	
	public Listing<Consumer<?>> getConsumers()
	{
		return consumers;
	}
	
	public ResolverCache<I, T> getCache()
	{
		return cache;
	}
	
	@Override
	public boolean matches(Listing<I> data)
	{
		boolean debug = DEBUG;
		
//		if(debug)
//		{
//			DEBUG = false;
//		}
		
		try
		{
			debug("matching data = " + data);
			resolve(data);
			debug("match data = " + data);
			return true;
		}
		catch(ResolverException e)
		{
			debug("not a match data = " + data);
			return false;
		}
		finally
		{
			DEBUG = debug;
		}
	}
	
	@Override
	public T resolve(Listing<I> data) throws ResolverException
	{
		T cache = getCache().get(data);
		if(cache != null)
		{
			return cache;
		}
		
		debug("!! start data: " + data);
		ConsumerStorage storage = resolveConsumer(0, data, new ConsumerStorage());
		debug("!! end" + "\n\n");
		
		return getCache().cache(data, resolve(storage));
	}
	
	private ConsumerStorage resolveConsumer(int c, Listing<I> data, ConsumerStorage storage) throws ResolverException
	{
		Consumer<?> consumer = getConsumers().get(c);
		
		if(c == getConsumers().size() - 1)
		{
			if(consumer.matches(data))
			{
				debug("final consumer data = " + data);
				updateStorage(consumer, data, storage);
				return storage;
			}
			else
			{
				debug("invalid final c = " + c + " data = " + data);
				throw new ResolverException("Invalid final consumer data: " + data);
			}
		}
		else
		{
			Consumer<?> next = getConsumers().get(c + 1);
			
			for(int i = 1; i < data.size(); i++)
			{
				Listing<I> sub = data.subList(0, i);
				
				boolean match = consumer.matches(sub);
				debug("check c = " + c + " consumer = " + consumer + " i = " + i + " sub = " + sub + " match = " + match);
				if(match)
				{
					if(i == data.size() - 1 || next.matches(data.subList(i)))
					{
						debug("passthrough i = " + i + " sub = " + sub + " post = " + data.subList(i + 1));
						updateStorage(consumer, sub, storage);
						return resolveConsumer(c + 1, data.subList(i), storage);
					}
				}
				else
				{
					debug("match drop i = " + i + " sub = " + sub);
					throw new ResolverException("Invalid consumer data: " + sub);
				}
			}
		}
		
		throw new ResolverException("Could not resolve consumer: " + consumer + " c = " + c + " data = " + data + " storage = " + storage);
	}
	
	private <K> void updateStorage(Consumer<K> consumer, Listing<I> data, ConsumerStorage storage) throws ResolverException
	{
		storage.set(consumer, consumer.resolve(data));
	}
	
	private void debug(String message)
	{
		if(DEBUG)
		{
			Debug.log("[" + getID() + "] " + message);
		}
	}
	
	public abstract T resolve(ConsumerStorage storage) throws ResolverException;
	
	public class Consumer<K>
	{
		private final ResolverCache<I, K> cache = new ResolverCache<>();
		
		private final IResolver<I, K> resolver;
		
		public Consumer(IResolver<I, K> resolver)
		{
			this.resolver = resolver;
			
			getConsumers().add(this);
		}
		
		public ResolverCache<I, K> getCache()
		{
			return cache;
		}
		
		public IResolver<I, K> getResolver()
		{
			return resolver;
		}
		
		public boolean matches(Listing<I> data)
		{
			if(getCache().isCached(data))
			{
				return true;
			}
			
			return getResolver().matches(data);
		}
		
		public K resolve(Listing<I> data) throws ResolverException
		{
			K cache = getCache().get(data);
			if(cache != null)
			{
				return cache;
			}
			
			return getCache().cache(data, getResolver().resolve(data));
		}
		
		@Override
		public String toString()
		{
			return "(" + getResolver().getClass().getSimpleName() + ")";
		}
	}
	
	public class ConsumerStorage
	{
		private final Mapping<Consumer<?>, Object> map = new Mapping<>();
		
		public <K> void set(Consumer<K> consumer, K value)
		{
			map.put(consumer, value);
		}
		
		@SuppressWarnings("unchecked")
		public <K> K get(Consumer<K> consumer) throws ResolverException
		{
			try
			{
				return (K)map.get(consumer);
			}
			catch(ClassCastException e)
			{
				throw new ResolverException(e);
			}
		}
		
		@Override
		public String toString()
		{
			return map.toString();
		}
	}
}
