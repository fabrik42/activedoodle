package activedoodle;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.*;
import javax.xml.parsers.*;
import javax.xml.validation.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import oauth.signpost.OAuth;
import oauth.signpost.basic.*;
import oauth.signpost.signature.*;

/**
 * ActiveDoodles version : 1.0
 * 
 * This class provides an easy way to communicate with the doodle.com REST API
 * in order to create, update and vote for polls.
 * 
 * More info can be found at: - www.doodle.com/xsd1/RESTfulDoodle.pdf -
 * http://doodle.com/xsd1/AAforRESTfulDoodle.pdf
 * 
 * You need doodle's credential to use API 
 * get one here https://doodle.com/mydoodle/consumer/credentials.html
 * 
 * @author Christian Bäuerlein christian.baeuerlein@gmail.com
 */
public class DoodlePoll
{

    /**
     * Logger for debugging
     */
    private static final Logger logger = Logger.getLogger(DoodlePoll.class.getName());

    /*
     * Your credentials for using the doodle.com REST API more info at
     * http://doodle.com/xsd1/AAforRESTfulDoodle.pdf
     */
    
    /**
     * Holds your consumer key for doodle oAuth authentification
     */
    private static String consumerKey = "ABC";

    /**
     * Holds your consumer secret for doodle oAuth authentification
     */
    private static String consumerSecret = "XYZ";

    /**
     * The base url of doodle
     */
    public final static String BASE_URL = "http://doodle.com";

    /**
     * REST API Path for requesting the oAuth request token
     */
    private final static String requestTokenURL = "/api1/oauth/requesttoken";

    /**
     * REST API Path for requesting the oAuth access token
     */
    private final static String accessTokenURL = "/api1/oauth/accesstoken";

    /**
     * REST API Path for authorizing a specific user (not in use)
     */
    private final static String userAuthorizationURL = "/api1/oauth/authorizeConsumer";
    /**
     * REST API Path for poll resources
     */
    public final static String pollsURL = "/api1/polls";

    /**
     * REST API Path for user resources
     */
    public final static String userURL = "/api1/user";

    /**
     * REST API Path for xsd definition
     */
    public final static String xsdURL = "/xsd1";

    /**
     * REST API Path for participants resources
     */
    public final static String participantsURL = "/participants";

    /**
     * The namespace URI of doodle generated xml files
     */
    private final static String namespaceUri = "http://doodle.com/xsd1";

    /**
     * oAuth consumer
     */
    private DefaultOAuthConsumer consumer;

    /**
     * Doodle oAuth service provider
     */
    private DefaultOAuthProvider serviceProvider;

    /**
     * Indicates if a valid oAuth token has been recieved
     */
    private boolean validOAuthToken = false;

    /**
     * Holds the HTTP status code of the last response of the doodle API
     */
    public int lastResponseCode;

    /**
     * Indicates if this is a newly generated doodle or an existing one. If
     * true, the poll gets posted when calling doodle.save(), if not the
     * existing poll gets updated.
     */
    private boolean isNewDoodle = false;

    /**
     * The secret doodle xKey
     */
    private String xDoodleKey = "";

    /**
     * The poll id of the current doodle poll
     */
    private String pollId;

    /**
     * The title of the poll
     */
    private String title;

    /**
     * The description of the poll
     */
    private String description;

    /**
     * The state of the poll (open vs. closed, not in use now)
     */
    private String state;

    /**
     * Constant for state open
     */
    public final static String STATE_OPEN = "OPEN";

    /**
     * Constant for state closed
     */
    public final static String STATE_CLOSED = "CLOSED";

    /**
     * Determines if the poll is either to find a meeting date or to choose
     * between different text options.
     */
    private String pollType;

    /**
     * Constant for a poll of type date
     */
    public final static String DATE_POLL = "DATE";

    /**
     * Constant for a poll of type text
     */
    public final static String TEXT_POLL = "TEXT";

    /**
     * Name of the initiator. Doodle API also allows to connect this with a user
     * id instead, but currently this is not supported.
     */
    private String initiator = "DORM SL2DOODLE";

    /**
     * Mode, the user can vote for an option
     * 
     * 2 = vote can be YES/NO 3 = vote can be YES/NO/MAYBE
     */
    private int optionLevels;

    /**
     * The participants of the doodle poll
     */
    private ArrayList<DoodlePollParticipant> participants = new ArrayList<DoodlePollParticipant>();;

    /**
     * The available options to vote for
     */
    private ArrayList<DoodlePollOption> options = new ArrayList<DoodlePollOption>();;

    // parsing
    private Element xmlPoll;
    private NodeList xmlOptions;
    private NodeList xmlParticipants;
    private Node xmlSingleParticipant;
    private NodeList xmlSinglePreferences;
    public Document doc;
    
   /**
    * Allow you to set credential before calling API
    * https://doodle.com/mydoodle/consumer/credentials.html
    * @param p_consumerKey the doodle's consumerKey
    * @param p_consumerPassword the doodle's password
    */
   public static void setCredential(String p_consumerKey,String p_consumerPassword)
   {
       consumerKey = p_consumerKey;
       consumerSecret = p_consumerPassword;
   }

    /**
     * Creates a new/empty doodle poll object and connects to the doodle REST
     * API via oAuth
     */
    public DoodlePoll()
    {
	isNewDoodle = true;

	connectOAuth();
    }

    /**
     * Creates a new doodle poll object based on an existing poll and tries to
     * populate it, by connecting to the doodle REST API and requesting the poll
     * data.
     * 
     * @param pollId
     *            The doodle poll id of the poll to be loaded.
     */
    public DoodlePoll(String pollId)
    {
	this.pollId = pollId;

	connectOAuth();

	populate();
    }

    /**
     * Indicates if this is a new poll (not saved at doodle) or an existing one.
     * 
     * @return True if new doodle, false if not.
     */
    public boolean isNew()
    {
	return isNewDoodle;
    }

    /**
     * Connects to the doodle REST API via oAuth using the provided credentials.
     * 
     * @return true, if access token was recieved, false if not.
     */
    private boolean connectOAuth()
    {
	try
	{
	    /*
	     * Round one, two-legged OAuth to access polls OAuth is just used to
	     * authenticate the consumer (you) against our server
	     */
	    consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
	    consumer.setMessageSigner(new HmacSha1MessageSigner());

	    serviceProvider = new DefaultOAuthProvider(BASE_URL + requestTokenURL, BASE_URL + accessTokenURL, BASE_URL
		    + userAuthorizationURL);

	    serviceProvider.retrieveRequestToken(consumer, OAuth.OUT_OF_BAND);
	    serviceProvider.retrieveAccessToken(consumer, null);

	    validOAuthToken = true;

	    return true;

	}
	catch (Exception e)
	{
	    e.printStackTrace();
	    return false;
	}

    }

    /**
     * Indicates, if the oAuth access token has already been recieved.
     * 
     * IMPORTANT: The tokens have a timeout - handling of this is not supported
     * yet.
     * 
     * @return True, if access token is valid, false if not.
     */
    public boolean isConnected()
    {
	return validOAuthToken;
    }

    /**
     * Returns the oAuth consumer
     * 
     * @return The oAuth consumer
     */
    public DefaultOAuthConsumer getConsumer()
    {
	return consumer;
    }

    /**
     * When the object should handle an existing doodle poll, this method is
     * called. It connects to the doodle REST API, requests the saved data of
     * the poll and populates the object with it.
     * 
     * @return True, if population was successful, false if not.
     */
    private boolean populate()
    {

	try
	{

	    HttpURLConnection request;

	    request = (HttpURLConnection) (new URL(BASE_URL + pollsURL + "/" + pollId)).openConnection();
	    consumer.sign(request);

	    // send the request
	    request.connect();

	    // response status should be 200 OK
	    // but it is not test
	    request.getResponseCode();

	    doc = streamToDocument(request.getInputStream());

	    xmlPoll = doc.getDocumentElement();

	    // extract poll meta data to object
	    this.title = xmlPoll.getElementsByTagNameNS(namespaceUri, "title").item(0).getTextContent();
	    this.description = xmlPoll.getElementsByTagNameNS(namespaceUri, "description").item(0).getTextContent();
	    this.state = xmlPoll.getElementsByTagNameNS(namespaceUri, "state").item(0).getTextContent()
		    .equalsIgnoreCase(STATE_OPEN) ? STATE_OPEN : STATE_CLOSED;
	    this.optionLevels = Integer.parseInt(xmlPoll.getElementsByTagNameNS(namespaceUri, "levels").item(0)
		    .getTextContent());
	    this.pollType = xmlPoll.getElementsByTagNameNS(namespaceUri, "type").item(0).getTextContent();

	    logger.info("poll title: " + title);
	    logger.info("poll description: " + description);
	    logger.info("poll state: " + state);
	    logger.info("poll optionLevels: " + optionLevels);
	    logger.info("poll pollType: " + pollType);

	    // extract options
	    xmlOptions = ((Element) xmlPoll.getElementsByTagNameNS(namespaceUri, "options").item(0))
		    .getElementsByTagNameNS(namespaceUri, "option");

	    logger.info(xmlOptions.getLength() + " possible options");

	    for (int i = 0; i < xmlOptions.getLength(); i++)
	    {
		String optionDate = null;
		if (this.pollType.equalsIgnoreCase(DoodlePoll.TEXT_POLL))
		{
		    optionDate = xmlOptions.item(i).getTextContent();
		}
		else if (this.pollType.equalsIgnoreCase(DoodlePoll.DATE_POLL))
		{
		    optionDate = xmlOptions.item(i).getAttributes().item(0).getTextContent();
		}
		options.add(new DoodlePollOption(optionDate, this));
	    }

	    // extract participants
	    xmlParticipants = ((Element) xmlPoll.getElementsByTagNameNS(namespaceUri, "participants").item(0))
		    .getElementsByTagNameNS(namespaceUri, "participant");

	    logger.info(xmlParticipants.getLength() + " Participants");

	    String p_name;
	    String pId;
	    int partLen = xmlParticipants.getLength();
	    for (int i = 0; i < partLen; i++)
	    {

		xmlSingleParticipant = xmlParticipants.item(i);

		p_name = ((Element) xmlSingleParticipant).getElementsByTagNameNS(namespaceUri, "name").item(0)
			.getTextContent();
		pId = ((Element) xmlSingleParticipant).getElementsByTagNameNS(namespaceUri, "id").item(0)
			.getTextContent();

		logger.info("Participant: " + p_name + " id: " + pId);

		DoodlePollParticipant p = new DoodlePollParticipant(this, p_name, pId);

		participants.add(p);

		xmlSinglePreferences = ((Element) ((Element) xmlSingleParticipant).getElementsByTagNameNS(namespaceUri,
			"preferences").item(0)).getElementsByTagNameNS(namespaceUri, "option");

		logger.info(xmlSinglePreferences.getLength() + " Preferences");

		DoodlePollOption currOption;

		for (int j = 0; j < xmlSinglePreferences.getLength(); j++)
		{
		    int vote = Integer.parseInt(xmlSinglePreferences.item(j).getTextContent());

		    currOption = options.get(j);

		    DoodlePollPreference pref = new DoodlePollPreference(p, currOption, vote);

		    p.preferences.add(pref);

		    currOption.preferences.add(pref);

		}

	    }

	    return true;

	}
	catch (Exception e)
	{
	    e.printStackTrace();
	    return false;
	}
    }
    
    /**
     * Return the list of all participants
     * @return  List<DoodlePollParticipant>
     */
    public List<DoodlePollParticipant> getParticpants()
    {
	return participants;
    }

    /**
     * Tries to find a participant, according to the name he attented to this
     * poll.
     * 
     * @param pName
     *            The name, the participant attented to this poll.
     * @return Null, if not found. Otherwise the participant.
     */
    public DoodlePollParticipant getParticipant(String pName)
    {

	for (int i = 0; i < participants.size(); i++)
	{

	    if (participants.get(i).name.equalsIgnoreCase(pName))
	    {
		return participants.get(i);
	    }
	}

	return null;
    }

    /**
     * Creates a new participant and adds them to the poll. By default, all the
     * preferences of the participant are set to "NO".
     * 
     * @param pName
     *            The name of the participant.
     * @return The new created participant. If a participant with the same name
     *         already exists, this one is returned and no new participant is
     *         created.
     */
    public DoodlePollParticipant addParticipant(String pName)
    {

	DoodlePollParticipant existing = getParticipant(pName);

	if (existing != null)
	{
	    return existing;
	}

	DoodlePollParticipant newParticipant = new DoodlePollParticipant(this, pName);
	DoodlePollOption currOption;

	participants.add(newParticipant);

	for (int i = 0; i < options.size(); i++)
	{

	    currOption = options.get(i);

	    DoodlePollPreference pref = new DoodlePollPreference(newParticipant, currOption,
		    DoodlePollPreference.OPTION_NO);

	    currOption.preferences.add(pref);

	    newParticipant.preferences.add(pref);
	}

	return newParticipant;
    }
    
    /**
     * Return all options available
     * @return List<DoodlePollOption>
     */
    public List<DoodlePollOption> getOptions()
    {
	return options;
    }

    /**
     * Gets an option based on its value.
     * 
     * @param value
     *            The value to search for
     * @return The option. If no option with this value exists, the method
     *         returns null.
     */
    public DoodlePollOption getOption(String value)
    {

	for (int i = 0; i < options.size(); i++)
	{

	    if (options.get(i).value.equalsIgnoreCase(value))
	    {
		return options.get(i);
	    }
	}

	return null;
    }

    /**
     * Adds a new option with the given value. If an option with this value
     * already exists, no new option is created and the existing option is
     * returned. By default, every existing user votes "NO" for the new option.
     * 
     * @param value
     *            The value of the option
     * @return The new created option (see desc)
     */
    public DoodlePollOption addOption(String value)
    {

	DoodlePollOption existing = getOption(value);

	if (existing != null)
	{
	    return existing;
	}

	DoodlePollOption newOption = new DoodlePollOption(value, this);
	DoodlePollParticipant currParticipant;

	options.add(newOption);

	for (int i = 0; i < participants.size(); i++)
	{

	    currParticipant = participants.get(i);

	    DoodlePollPreference pref = new DoodlePollPreference(currParticipant, newOption,
		    DoodlePollPreference.OPTION_NO);

	    currParticipant.preferences.add(pref);

	    currParticipant.touch();

	    newOption.preferences.add(pref);
	}

	return newOption;
    }

    /**
     * Creates the xml representation of the poll for the doodle REST API.
     * 
     * @return The xml doc for the doodle REST API as a string
     */
    public String createPollXml()
    {

	StringBuffer pollXml = new StringBuffer();

	pollXml.append("<poll xmlns=\"http://doodle.com/xsd1\">");
	pollXml.append("<type>" + pollType + "</type>");
	pollXml.append("<levels>");
	pollXml.append(optionLevels);
	pollXml.append("</levels>");
	pollXml.append("<title>" + title + "</title>");
	pollXml.append("<description>" + description + "</description>");
	pollXml.append("<initiator><name>" + initiator + "</name></initiator>");
	// add the options
	pollXml.append("<options>");
	for (int i = 0; i < options.size(); i++)
	{
	    if (pollType.equalsIgnoreCase(DATE_POLL))
	    {
		pollXml.append("<option dateTime=\"" + options.get(i).value + "\"/>");
	    }
	    else if (pollType.equalsIgnoreCase(TEXT_POLL))
	    {
		pollXml.append("<option>" + options.get(i).value + "</option>");
	    }
	}
	pollXml.append("</options>");
	pollXml.append("</poll>");

	return pollXml.toString();
    }

    /**
     * Saves the poll to the doodle REST API. Depending on if this is a new
     * created poll or not, the data is either send as a POST (save new) or a
     * PUT (save existing) request.
     * 
     * @return True, if the request was successful, false, if not.
     */
    public boolean save()
    {
	HttpURLConnection http;
	OutputStream body;

	String xmlPollString = createPollXml();

	if (isNewDoodle)
	{
	    // post
	    try
	    {

		http = (HttpURLConnection) (new URL(BASE_URL + pollsURL)).openConnection();

		http.setDoOutput(true);

		http.setRequestProperty("Content-Type", "text/xml");

		http.setRequestMethod("POST");

		consumer.sign(http);

		body = http.getOutputStream();

		body.write(xmlPollString.getBytes());

		body.close();
		pollId = http.getHeaderField("Content-Location");
		xDoodleKey = http.getHeaderField("X-DoodleKey");

		logger.info("Poll ID is " + pollId);
		logger.info("Admin key is " + xDoodleKey);

		lastResponseCode = http.getResponseCode();

		logger.info("New doodle was created: " + pollId + " response: " + lastResponseCode);

		if (lastResponseCode == HttpURLConnection.HTTP_CREATED)
		{

		    setXKey(xDoodleKey);

		    return true;
		}
		else
		{
		    return false;
		}

	    }
	    catch (Exception e)
	    {
		e.printStackTrace();
		return false;
	    }

	}
	else
	{
	    // put
	    try
	    {

		http = (HttpURLConnection) (new URL(BASE_URL + pollsURL + "/" + pollId)).openConnection();

		http.setInstanceFollowRedirects(false);

		http.setDoOutput(true);
		http.setRequestProperty("Content-Type", "text/xml");
		http.setRequestMethod("PUT");
		http.setRequestProperty("X-DoodleKey", getXKey());
		consumer.sign(http);
		body = http.getOutputStream();
		body.write(xmlPollString.getBytes());
		body.close();
		lastResponseCode = http.getResponseCode();

		if (lastResponseCode == HttpURLConnection.HTTP_OK)
		{

		    logger.info("Doodle was saved: " + pollId);

		    return true;

		}
		else
		{
		    return false;
		}

	    }
	    catch (Exception e)
	    {
		e.printStackTrace();
		return false;
	    }
	}
    }

    /**
     * Converts the xml response of the doodle REST API to a xml document.
     * 
     * @param xmlStream
     *            The input stream of a doodle REST API response.
     * @return The xml doc
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private static Document streamToDocument(InputStream xmlStream) throws SAXException, IOException,
	    ParserConfigurationException
    {
	SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	Schema s = sf.newSchema(new URL(BASE_URL + xsdURL + "/poll.xsd"));
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setIgnoringComments(true);
	dbf.setIgnoringElementContentWhitespace(true);
	dbf.setNamespaceAware(true);
	dbf.setSchema(s);
	DocumentBuilder db = dbf.newDocumentBuilder();
	return db.parse(xmlStream);
    }

    /**
     * Saves the secret doodle xKey. The xKey is needed to update existing
     * polls.
     * 
     * @param xDoodleKey
     *            The xKey to be saved.
     */
    private void setXKey(String xDoodleKey)
    {
	this.xDoodleKey = xDoodleKey;
    }

    /**
     * Returns the secret doodle xKey of this poll.
     * 
     * @return The doodle xKey.
     */
    public String getXKey()
    {
	return xDoodleKey;
    }

    /**
     * Set the title of the poll.
     * 
     * @param title
     *            The new title.
     */
    public void setTitle(String title)
    {
	this.title = title;
    }

    /**
     * Returns the title of this poll.
     * 
     * @return The title of this poll.
     */
    public String getTitle()
    {
	return this.title;
    }

    /**
     * Set the description of this poll.
     * 
     * @param desc
     *            The new description.
     */
    public void setDesc(String desc)
    {
	this.description = desc;
    }

    /**
     * Returns the description of the poll.
     * 
     * @return The description of this poll.
     */
    public String getDesc()
    {
	return this.description;
    }

    /**
     * Set the type of this poll. Use constants DoodlePoll.DATE_POLL or
     * DoodlePoll.TEXT_POLL
     * 
     * @param desc
     *            The new type.
     */
    public void setType(String t)
    {
	this.pollType = t;
    }

    /**
     * Returns the type of the poll.
     * 
     * @return The type of this poll. Either DoodlePoll.DATE_POLL or
     *         DoodlePoll.TEXT_POLL
     */
    public String getType()
    {
	return this.pollType;
    }

    /**
     * Sets the mode of this poll.
     * 
     * @param m
     *            Either 2 for: YES/NO Or 3 for: YES/NO/MAYBE
     */
    public void setMode(int m)
    {
	this.optionLevels = m;
    }

    /**
     * Returns the mode of this poll.
     * 
     * @return Either 2 for: YES/NO Or 3 for: YES/NO/MAYBE
     */
    public int getMode()
    {
	return this.optionLevels;
    }

    /**
     * Returns the doodle id of this poll.
     * 
     * @return The doodle id of this poll.
     */
    public String getId()
    {
	return this.pollId;
    }

}
