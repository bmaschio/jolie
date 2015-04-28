package joliex.c_sharp;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import joliex.c_sharp.formatExeption;

public class Jolie2C_SharpCommandLineParser extends CommandLineParser {

    private String namespace;

    private String targetPort;
    private boolean addSource = false;

    public String getNamespace()
	    throws formatExeption {
	return namespace;
    }


    public String getTargetPort() {
	return targetPort;
    }

    public boolean isAddSource() {
	return addSource;
    }

    public void setAddSource(boolean addSource ) {
	this.addSource = true;
    }

    private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler {

	private String namespace;

	private String targetPort;
	private boolean addSource = false;

	public int onUnrecognizedArgument(List< String> argumentsList, int index)
		throws CommandLineException {
	    if ("--addSource".equals(argumentsList.get(index))) {
		index++;
		this.addSource = true;
	    } else if ("--nameSpace".equals(argumentsList.get(index))) {
		index++;
		namespace = argumentsList.get(index);
	    } else if ("--targetPort".equals(argumentsList.get(index))) {
		index++;
		targetPort = argumentsList.get(index);
	    } else {
		throw new CommandLineException("Unrecognized command line option: " + argumentsList.get(index));
	    }

	    return index;
	}
    }

    public static Jolie2C_SharpCommandLineParser create(String[] args, ClassLoader parentClassLoader)
	    throws CommandLineException, IOException {
	return new Jolie2C_SharpCommandLineParser(args, parentClassLoader, new JolieDummyArgumentHandler());
    }

    private Jolie2C_SharpCommandLineParser(String[] args, ClassLoader parentClassLoader, JolieDummyArgumentHandler argHandler)
	    throws CommandLineException, IOException {
	super(args, parentClassLoader, argHandler);

	namespace = argHandler.namespace;
	targetPort = argHandler.targetPort;
	addSource = argHandler.addSource;
    }

    @Override
    protected String getHelpString() {
	return "Usage: jolie2c_sharp --addSource true --namespace namespace in c# [--targetPort inputPort_to_be_encoded] file.ol";
    }
}
