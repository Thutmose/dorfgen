package MappedXML;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedTruncate {

	static private final long BUFFER_SIZE = 1024;
	
	/**
	 * Truncates a file at the first occurrence of a string.
	 * @param inPath Path and filename of input file
	 * @param outPath Path and filename of output file
	 * @param readTo String at which to truncate the file
	 * @param truncateLast If true, will truncate before readTo
	 */
	public static void ReadTruncateAndOutput(String inPath, String outPath, String readTo, boolean truncateLast) {
		
		ReadTruncateAndOutput(inPath, outPath, readTo, "", truncateLast);
	}
	
	/**
	 * Truncates a file at the first occurrence of a string.
	 * Concatenates a string after the truncation.
	 * @param inPath Path and filename of input file
	 * @param outPath Path and filename of output file
	 * @param readTo String at which to truncate the file
	 * @param append String to append after the truncated text
	 */
	public static void ReadTruncateAndOutput(String inPath, String outPath, String readTo, String append, boolean truncateLast) {
		
		try(Writer out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outPath), "utf-8"))) {
			
			try(FileInputStream in = new FileInputStream(inPath)) {
				
				InnerProcess(in, out, readTo, truncateLast);
				
				in.close();
				
				out.write(append.toCharArray());
				
				out.close();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void InnerProcess(FileInputStream in, Writer out, String readTo, boolean truncateLast) {
		
		FileChannel inChannel = in.getChannel();
		MappedByteBuffer inMap;
		String s, subs;
		long size, bufferMax;
		int pos;
		
		try {
			size = inChannel.size();
//			System.out.println("Size of input is " + size);
		} catch(Exception e) {
			e.printStackTrace();
			return;
		}
		
		bufferMax = MappedTruncate.BUFFER_SIZE;
		
		if(size < MappedTruncate.BUFFER_SIZE) {
			bufferMax = size;
		}
		boolean done = false;
		for(long i = 0; i < size; i += bufferMax) {
			
			if(bufferMax > size - i) bufferMax = size - i;
			
			try {
				inMap = inChannel.map(FileChannel.MapMode.READ_ONLY, i, bufferMax);
			} catch(Exception e) {
				e.printStackTrace();
				return;
			}
			
//			System.out.println("Reading " + bufferMax + " bytes of " + i + "/" + size);
			
			s = new String();
			
			for(@SuppressWarnings("unused")
            int j = 0; inMap.remaining() != 0; j++)
			{
				s += (char) inMap.get();
			}
			
			pos = s.indexOf(readTo);
            done = pos != -1;
			
			if(!done) {
				subs = s;
			} else {
                subs = s.substring(0, pos + (truncateLast ? readTo.length() : 0));
			}
			
			try {
				out.write(subs.toCharArray());
			} catch(Exception e) {
				e.printStackTrace();
				return;
			}
			
			if(done) break;
        }
    }
}