package net.anasa.util.data.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import net.anasa.util.data.DataConform.FormatException;
import net.anasa.util.data.format.IDataFormat;

public class FileHandler<T> implements IHandler<T>
{
	private final File file;
	private final IDataFormat<T> format;
	
	public FileHandler(File file, IDataFormat<T> format)
	{
		this.file = file;
		this.format = format;
	}
	
	public File getFile()
	{
		return file;
	}

	public IDataFormat<T> getFormat()
	{
		return format;
	}

	@Override
	public T read() throws IOException
	{
		getFile().getParentFile().mkdirs();
		getFile().createNewFile();
		
		if(!getFile().isFile())
		{
			throw new IOException("Invalid file: " + getFile());
		}
		
		try(BufferedReader reader = new BufferedReader(new FileReader(getFile())))
		{
			String data = "";
			while(reader.ready())
			{
				data += reader.readLine() + '\n';
			}
			
			return getFormat().getFrom(data);
		}
		catch(FormatException e)
		{
			throw new IOException(e);
		}
	}
	
	@Override
	public void write(T data) throws IOException
	{
		try(PrintWriter output = new PrintWriter(new FileWriter(getFile())))
		{
			output.print(getFormat().getFormatted(data));
		}
		catch(FormatException e)
		{
			throw new IOException(e);
		}
	}
}
