package org.pititom.core.args4j;


import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.pititom.core.Service;

/**
 * {@link Class} {@link OptionHandler}.
 *
 * @author Thomas Pérennou
 */
public class ClassOptionHandler extends OptionHandler<Class<?>> {
    public ClassOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Class<?>> setter) {
        super(parser, option, setter);
    }

    @Override
    public int parseArguments(Parameters params) throws CmdLineException {
        try {
	        setter.addValue(Service.getInstance().loadClass(params.getParameter(0)));
        } catch (ClassNotFoundException exception) {
	        throw new CmdLineException(this.owner, exception);
        }
        return 1;
    }

    @Override
    public String getDefaultMetaVariable() {
        return "CLASS";
    }
}
