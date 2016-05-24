package org.osm2world.console;

import static com.google.common.base.Preconditions.*;
import static org.osm2world.console.CLIArgumentsUtil.ProgramMode.CONVERT;

import java.util.ArrayList;
import java.util.List;

/**
 * a group of {@link CLIArguments} that represent conversions
 * which can be performed at the same time
 * and are only distinguished by output format/parameters
 */
public class CLIArgumentsGroup {
	
	private final CLIArguments representative;
	private final List<CLIArguments> cliArgumentsList;
	
	/**
	 * @param representative  the first member of the group.
	 */
	public CLIArgumentsGroup(CLIArguments representative) {
		
		checkNotNull(representative);
		
		this.representative = representative;
		
		cliArgumentsList = new ArrayList<CLIArguments>();
		cliArgumentsList.add(representative);
		
	}
	
	public void addCLIArguments(CLIArguments cliArguments) {
		
		checkNotNull(cliArguments);
		checkArgument(isCompatible(cliArguments), "argument incompatible with group");
		
		cliArgumentsList.add(cliArguments);
		
	}

	public CLIArguments getRepresentative() {
		return representative;
	}
	
	public List<CLIArguments> getCLIArgumentsList() {
		return cliArgumentsList;
	}
	
	/**
	 * checks whether a CLIArguments instance is compatible with this group
	 */
	public boolean isCompatible(CLIArguments cliArguments) {
		return isCompatible(representative, cliArguments);
	}

	/**
	 * checks whether two CLIArguments instances can be put into the same group
	 */
	private static final boolean isCompatible(
			CLIArguments args1, CLIArguments args2) {
		
		return CLIArgumentsUtil.getProgramMode(args1) == CONVERT
			&& CLIArgumentsUtil.getProgramMode(args2) == CONVERT
			&& bothNullOrEqual(args1.getInputMode(), args2.getInputMode())
			&& bothNullOrEqual(args1.getInput(), args2.getInput())
			&& bothNullOrEqual(args1.getInputQuery(), args2.getInputQuery())
			&& bothNullOrEqual(args1.getInputBoundingBox(), args2.getInputBoundingBox())
			&& bothNullOrEqual(args1.getOverpassURL(), args2.getOverpassURL())
			&& ((args1.isConfig() && args1.getConfig().equals(args2.getConfig()))
					|| (!args1.isConfig() && !args2.isConfig()));
		
	}
	
	private static final boolean bothNullOrEqual(Object o1, Object o2) {
		return (o1 == null && o2 == null)
				|| (o1 != null && o1.equals(o2));
	}

}
