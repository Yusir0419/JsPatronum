package com.qiqijin.jspatronum;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.sanityinc.jargs.CmdLineParser;

/**
 *
 * 代码混淆器的入口类
 * 
 * @author Gin
 */
public class Main {
    public static void main(String args[]) {
	// 命令行输入处理
	CmdLineParser parser = new CmdLineParser();
	CmdLineParser.Option<Boolean> helpOpt = parser.addBooleanOption('h', "help");
	CmdLineParser.Option<Boolean> versionOpt = parser.addBooleanOption('v', "version");
	CmdLineParser.Option<String> outputFilenameOpt = parser.addStringOption('o', "output");
	CmdLineParser.Option<Boolean> compressOpt = parser.addBooleanOption('c', "compress");
	CmdLineParser.Option<Boolean> disableConsoleOpt = parser.addBooleanOption('d', "disable-console");
	CmdLineParser.Option<String> bindHostNameOpt = parser.addStringOption('b', "bind-hostname");
	CmdLineParser.Option<Boolean> controlFlowOpt = parser.addBooleanOption('f', "control-flow");
	Reader in = null;
	Writer out = null;

	try {
	    parser.parse(args);
	    Boolean help = (Boolean) parser.getOptionValue(helpOpt);
	    if (help != null && help.booleanValue()) {
		usage();
		System.exit(0);
	    }

	    Boolean version = (Boolean) parser.getOptionValue(versionOpt);
	    if (version != null && version.booleanValue()) {
		nowVersion();
		System.exit(0);
	    }

	    Boolean compress = (Boolean) parser.getOptionValue(compressOpt);
	    Boolean disableConsole = (Boolean) parser.getOptionValue(disableConsoleOpt);
	    Boolean controlFlow = (Boolean) parser.getOptionValue(controlFlowOpt);
	    String hostName = (String) parser.getOptionValue(bindHostNameOpt);
	    // 输入
	    String[] fileArgs = parser.getRemainingArgs();
	    java.util.List<String> files = java.util.Arrays.asList(fileArgs);
	    // 如果命令行没有文件输入，则从终端读取
	    if (files.isEmpty()) {
		System.out.print("Please input file names: ");
		files = new java.util.ArrayList<String>();
		files.add("-");
	    }

	    // 输出
	    String output = (String) parser.getOptionValue(outputFilenameOpt);
	    String pattern[];
	    if (output == null) {
		pattern = new String[0];
	    } else if (output.matches("(?i)^[a-z]\\:\\\\.*")) { // if output is with something like c:\ dont
		// split it
		pattern = new String[] { output };
	    } else {
		pattern = output.split(":");
	    }

	    // 保存输入文件内容
	    java.util.Iterator<String> filenames = files.iterator();
	    while (filenames.hasNext()) {
		String inputFilename = (String) filenames.next();
		if (inputFilename.equals("-")) {
		    in = new InputStreamReader(System.in);
		} else {
		    in = new InputStreamReader(new FileInputStream(inputFilename));
		}

		//
		String outputFilename = output;
		if (pattern.length > 1 && files.size() > 0) {
		    outputFilename = inputFilename.replaceFirst(pattern[0], pattern[1]);
		}

		final String localFilename = inputFilename;
		Obfuscator obfuscator = new Obfuscator(in, compress, disableConsole, hostName, controlFlow,
		    new ErrorReporter() {
		    public void warning(String message, String sourceName, int line,
			String lineSource,
			int lineOffset) {
			System.err.println("\n[WARNING] in " + localFilename);
			if (line < 0) {
			    System.err.println("  " + message);
			} else {
			    System.err.println("  " + line + ':'
					       + lineOffset + ':' + message);
			}
		    }

		    public void error(String message, String sourceName, int line,
			String lineSource, int lineOffset) {
			System.err.println("[ERROR] in " + localFilename);
			if (line < 0) {
			    System.err.println("  " + message);
			} else {
			    System.err.println("  " + line + ':'
					       + lineOffset + ':' + message);
			}
		    }

		    public EvaluatorException runtimeError(String message,
			String sourceName, int line,
			String lineSource, int lineOffset) {
			error(message, sourceName, line, lineSource,
			    lineOffset);
			return new EvaluatorException(message);
		    }
		    });

		// 关闭输入流，打开输出流，防止输入文件被覆盖
		in.close();
		in = null;

		if (outputFilename == null) {
		    out = new OutputStreamWriter(System.out);
		} else {
		    out = new OutputStreamWriter(new FileOutputStream(outputFilename));
		}
		obfuscator.obfuscate();
		obfuscator.compress(out);
	    }
	} catch (CmdLineParser.OptionException e) {
	    e.printStackTrace();
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }

	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    private static void nowVersion() {
	System.out.println("JsPatronum Version: 3.14");
    }

    /*
     * 打印帮助信息
     */
    private static void usage() {
	System.err.println(
	    "\nJsPatronum Version: 3.14\n"
	    + "\nUsage: java -jar JsPatronum.jar [options] [input file]\n"
	    + "\n"
	    + "Global Options\n"
	    + "  -v, --version				Print version information\n"
	    + "  -h, --help				Displays this information\n"
	    + "  -o <file>				Place the output into <file>. Defaults to stdout.\n"
	    + "				Multiple files can be processed using the following syntax:\n"
	    + "				java -jar JsPatronum.jar -o '.js$:-new.js' *.js\n\n"
	    + "Additional Options\n"
	    + " -c, --compress                         Compress code size\n"
	    + " -d, --disable-console                  Disable console debugging\n"
	    + " -f, --control-flow                     Enable control flow obfuscation\n"
	    + " -b <hostname>, --bind-hostname         Bind hostname\n "
	    + "				If you want to use all functions: \n"
	    + "				java -jar JsPatronum.jar -c -d -f -b 'localhost' -o '.js$:-new.js' *.js\n");
    }
}
