package activedoodle;

/**
 * 	A preference represents the vote of a user for
 * 	a single option of a poll.
 * 
 * 	The default vote for an option is always NO.
 * 
 * 	@author Christian Bäuerlein christian.baeuerlein@gmail.com
 */
public class DoodlePollPreference {

	/**
	 * 	The participant who set this preference.
	 */
	public DoodlePollParticipant participant;
	
	/**
	 * 	The option the participant voted for.
	 */
	public DoodlePollOption option;
	
	/**
	 * 	Constant for the vote YES
	 */
	public final static int OPTION_YES = 2;
	
	/**
	 * 	Constant for the vote MAYBE
	 * 	(only possible, if parent doodle poll has the mode 3)
	 */
	public final static int OPTION_MAYBE = 1;
	
	/**
	 * 	Constant for the vote NO
	 */
	public final static int OPTION_NO = 0;
	
	/**
	 * 	The vote of the participant for the option.
	 */
	private int vote;
	
	/**
	 * 	Creates a new preference.
	 * 
	 * 	@param participant
	 * 		The participant this preference is from.
	 * 	
	 * 	@param option
	 * 		The option the participant sets this preference to.
	 * 	
	 * 	@param vote
	 * 		The vote of the participant for the passed option.
	 */
	public DoodlePollPreference(DoodlePollParticipant participant, DoodlePollOption option, int vote){
		this.participant = participant;
		this.option = option;		
		this.vote = vote;		
	}
	
	/**
	 * 	Sets the vote for this preference
	 * 
	 * 	@param vote
	 * 		Should be:
	 * 		DoodlePollPreference.OPTION_YES or
	 * 		DoodlePollPreference.OPTION_NO or
	 * 		DoodlePollPreference.OPTION_MAYBE (only possible, if parent doodle poll has the mode 3)
	 */
	public void setVote(int vote) {		
		this.vote = vote;	
		participant.touch();		
	}
	
	/**
	 * 	Returns the vote of this preference
	 * 
	 * 	@return
	 * 		Should be:
	 * 		DoodlePollPreference.OPTION_YES or
	 * 		DoodlePollPreference.OPTION_NO or
	 * 		DoodlePollPreference.OPTION_MAYBE (only possible, if parent doodle poll has the mode 3)
	 */
	public int getVote() {
		return vote;
	}

}
