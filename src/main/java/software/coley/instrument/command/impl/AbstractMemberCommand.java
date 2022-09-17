package software.coley.instrument.command.impl;

import software.coley.instrument.command.AbstractCommand;

/**
 * Common outline for commands dealing with member interactions.
 *
 * @author Matt Coley
 */
public abstract class AbstractMemberCommand extends AbstractCommand {
	protected String owner;
	protected String name;
	protected String desc;

	/**
	 * @param key
	 * 		Command identifier.
	 */
	protected AbstractMemberCommand(int key) {
		super(key);
	}

	/**
	 * @return Member owner.
	 */
	public String getOwner() {
		return owner.replace('.', '/');
	}

	/**
	 * @param owner
	 * 		Member owner.
	 */
	public void setOwner(String owner) {
		this.owner = owner.replace('/', '.');
	}

	/**
	 * @return Member name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * 		Member name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Member descriptor.
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @param desc
	 * 		Member descriptor.
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}
}
