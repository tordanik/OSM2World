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
			&& args1.getInput().equals(args2.getInput())
			&& ((args1.isConfig() && args1.getConfig().equals(args2.getConfig()))
					|| (!args1.isConfig() && !args2.isConfig()));
				
	}

}
