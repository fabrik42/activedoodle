# Active Doodle #

These classes provide a simple, object oriented way to create,
update and vote for polls hosted at the doodle.com platform using Java.

## Dependencies ##

  * [oauth-signpost lib](http://code.google.com/p/oauth-signpost/)
  * One of the HTTP libraries that can be used with oauth-signpost (see [supported HTTP Libraries](http://code.google.com/p/oauth-signpost/wiki/SupportedHttpLibraries))


## Set up ##

### 1. Download ###

You can download source from svn repos and dependencies
or the last version Jar http://code.google.com/p/activedoodle/downloads/list but you couldn't edit source in this case

### 2. Implement two simple methods ###

Every Doodle poll has a unique id. But in addition to this, every poll has also a so called **doodle xKey**. In order to save changes on the poll to the Doodle API you need to have both, the id, as well as the xKey (both are returned when you create a poll using the API/these classes).

So to make this wrapper work properly you have to change the methods

`private void DoodlePoll.setXKey(String xDoodleKey)`

and

`public String DoodlePoll.getXKey()`

to a persistent storage mechanism of your choice.  You can basically do this any way you want, using files, a database or whatever.


Tip: These classes work great with [Google's App Engine](http://code.google.com/appengine), which also offers a persistent storage.

Tip: Since the new version, these function save values in a parameter. So it is possible to not implements these method if you call setXKey before using other functions.

### 3. Use it! ###

Now you can communicate with the Doodle API like this:

**Use your own credentials**

get credential here https://doodle.com/mydoodle/consumer/credentials.html

```
DoodlePoll.setCredential("consumerKey value", "consumerSecret value");
```

**Create a new Doodle poll**

```
// create a new poll
DoodlePoll poll = new DoodlePoll();

// set title, description
poll.setTitle("What do you want for lunch?");
poll.setDesc("Just a test poll..");

// set the poll type to text
poll.setType(DoodlePoll.TEXT_POLL);
// only YES or NO are possible votes
poll.setMode(2);

// add options to vote for
poll.addOption("pizza");
poll.addOption("pasta");
poll.addOption("burger");

// save the poll to Doodle
poll.save()
```


**Update existing poll**

```
// request data of existing poll from Doodle API
DoodlePoll poll = new DoodlePoll("myPollId");

// create new participant
DoodlePollParticipant user = poll.addParticipant("ELVIS");

// get an existing option
DoodlePollOption opt = poll.getOption("pizza");

// get the user's vote for this option
DoodlePollPreference pref = opt.getPreference(user);

// change the voting of the participant to YES
pref.setVote(DoodlePollPreference.OPTION_YES);

// save changes to the user
user.save();
```

### Further Resources ###

More Informations about the Doodle.com REST API can be found here:

**[Doodle REST API Documentation](http://www.doodle.com/xsd1/RESTfulDoodle.pdf)**

**[Doodle API oAuth Informations](http://doodle.com/xsd1/AAforRESTfulDoodle.pdf)**


## Enjoy! ##