package org.osm2world.console;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
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
	static final boolean isCompatible(
			CLIArguments args1, CLIArguments args2) {

		return CLIArgumentsUtil.getProgramMode(args1) == CONVERT
			&& CLIArgumentsUtil.getProgramMode(args2) == CONVERT

			&& (args1.isInputMode()
				? args2.isInputMode() && args1.getInputMode().equals(args2.getInputMode())
				: !args2.isInputMode())

			&& (args1.isInput()
				? args2.isInput() && args1.getInput().equals(args2.getInput())
				: !args2.isInput())

			&& (args1.isInputQuery()
				? args2.isInputQuery() && args1.getInputQuery().equals(args2.getInputQuery())
				: !args2.isInputQuery())

			&& (args1.isInputBoundingBox()
				? args2.isInputBoundingBox() && args1.getInputBoundingBox().equals(args2.getInputBoundingBox())
				: !args2.isInputBoundingBox())

			&& (args1.isTile()
					? args2.isTile() && args1.getTile().equals(args2.getTile())
					: !args2.isTile())

			&& (args1.isOverpassURL()
				? args2.isOverpassURL() && args1.getOverpassURL().equals(args2.getOverpassURL())
				: !args2.isOverpassURL())

			&& args1.getConfig().equals(args2.getConfig());

	}

}
