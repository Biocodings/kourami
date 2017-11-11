/*
Part of Kourami HLA typer/assembler
(c) 2017 by  Heewook Lee, Carl Kingsford, and Carnegie Mellon University.
See LICENSE for licensing.
*/
import java.io.*;

public class LogHandler{

    public static boolean debug = true;
    
    public StringBuffer bf;
    
    public BufferedWriter bw;

    public LogHandler(){
	this.bf = new StringBuffer();
	this.bw = null;
    }

    public void flush(){
	try{
	    if(this.bw == null){
		this.bw = new BufferedWriter(new FileWriter(HLA.OUTPREFIX + ".log"));
	    }
	    this.bw.write(this.bf.toString());
	    this.bw.flush();
	    this.bf = new StringBuffer();
	}catch(IOException ioe){
	    ioe.printStackTrace();
	}
    }

    public void appendln(int i){
	bf.append(i + "\n");
    }

    public void appendln(String line){
	bf.append(line + "\n");
    }

    public void appendln(char c){
	bf.append(c + "\n");
    }
    
    public void appendln(){
	bf.append("\n");
    }
    
    public void append(int i){
	bf.append(i + "");
    }

    public void append(String line){
	bf.append(line);
    }

    public void append(char c){
	bf.append(c + "");
    }

    
    public void outToFile(){
	
	try{
	    if(bw == null)
		this.flush();
	    bw.write(bf.toString());
	    bw.close();
	}catch(IOException ioe){
	    ioe.printStackTrace();
	}
    }
    
}
