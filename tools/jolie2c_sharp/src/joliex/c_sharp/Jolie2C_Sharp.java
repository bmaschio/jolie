/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.c_sharp;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.CommandLineException;

import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import joliex.c_sharp.impl.C_SharpDocumentCreator;
import joliex.c_sharp.impl.ProgramVisitor;

/**
 *
 * @author balint
 */
public class Jolie2C_Sharp {

    public static void main(String[] args) {
	try {

	    Jolie2C_SharpCommandLineParser cmdParser = Jolie2C_SharpCommandLineParser.create(args, Jolie2C_Sharp.class.getClassLoader());

	    Program program = ParsingUtils.parseProgram(
		    cmdParser.programStream(),
		    URI.create("file:" + cmdParser.programFilepath()),
		    cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants());

	    //Program program = parser.parse();
	    ProgramInspector inspector = ParsingUtils.createInspector(program);
	    ProgramVisitor visitor = new ProgramVisitor(program);
	    visitor.run();
	    C_SharpDocumentCreator documentJava = new C_SharpDocumentCreator(inspector, cmdParser.getNamespace(), cmdParser.getTargetPort(), cmdParser.isAddSource());
	    documentJava.ConvertDocument();

	} catch (formatExeption ex) {
	    Logger.getLogger(Jolie2C_Sharp.class.getName()).log(Level.SEVERE, null, ex);
	} catch (CommandLineException e) {
	    System.out.println(e.getMessage());
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (ParserException e) {
	    e.printStackTrace();
	} catch (SemanticException e) {
	    e.printStackTrace();
	} 
    }

}
