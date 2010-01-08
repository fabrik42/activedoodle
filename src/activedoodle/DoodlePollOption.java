package activedoodle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * 	Instances of this class represent options of a doodle poll,
 * 	that participants can vote for.
 * 
 * 	There are two different types of options: Text or Date.
 * 	Basically the only difference between them is that values
 * 	for date polls need to be in a special format (see datePattern)
 * 	and the notation in the doodle API xml files are different.
 * 
 *	@author Christian Bäuerlein christian.baeuerlein@gmail.com
 */
public class DoodlePollOption {

	/**
	 * 	The value/caption of this option
	 */
	public String value;
	/**
	 * 	In case, the parent poll is of type Date,
	 * 	this holds the value in a calendar format.
	 */
	private Calendar date;
	
	/**
	 * 	The date pattern to parse the date string.
	 */
	private static final String datePattern = "yyyy-MM-dd'T'HH:mm:ss";
	
	/**
	 * 	The votes for this option
	 */
	public ArrayList<DoodlePollPreference> preferences;
	
	/**
	 * 	The parent doodle poll
	 */
	private DoodlePoll poll; 
	
	/**
	 * 	Creates a new poll option.
	 * 
	 * 	@param value
	 * 		The value/caption of the poll option.
	 * 		In case the parent poll is of type Date,
	 * 		the option value must match the datePattern.
	 * 	@param p
	 * 		The parent doodle poll
	 */
	public DoodlePollOption(String value, DoodlePoll p) {
	
		try {
			
			this.poll = p;
			
			preferences = new ArrayList<DoodlePollPreference>();

			this.value = value;
			
			if(poll.getType().equalsIgnoreCase(DoodlePoll.DATE_POLL)) {
	
				SimpleDateFormat sdfInput = new SimpleDateFormat( datePattern ) ;  
			 				
				date = new GregorianCalendar();
	
				date.setTime(sdfInput.parse ( value ));
				
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
	
	/**
	 * 	Returns the preference/vote of the given participant
	 * 	for this option.
	 * 
	 * 	@param participant
	 * 		The participant to get the vote for.
	 * 
	 * 	@return
	 * 		The preference of the passed participant for this option.			
	 */
	public DoodlePollPreference getPreference(DoodlePollParticipant participant) {
		
		for(int i = 0; i < preferences.size(); i++) {
			
			if(preferences.get(i).participant.equals(participant)) {
				return preferences.get(i);
			}			
		}
		
		return null;	
	}	
}
