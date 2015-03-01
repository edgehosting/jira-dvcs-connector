Overview
========

With the Bitbucket and JIRA plugin you can create unlimited private repositories with full-featured issue and project management support. And it takes less than 30 seconds to configure. Bring your code to the next level with Bitbucket and JIRA.

* `30 second overview video`_
* `Install the JIRA DVCS Connector`_

.. image:: http://blog.bitbucket.org/wp-content/uploads/2011/06/Bitbucket-JIRA-tab1.png
    :align: center

Features
========

* Track changesets, monitor source code edits, and link back to Bitbucket.
* Push your changesets to JIRA by referencing issue keys in your commit messages.
* Map unlimited public and private Bitbucket repositories to your JIRA projects. 
* Full setup and configuration in under 1 minute.

.. _`Install the JIRA DVCS Connector`: https://plugins.atlassian.com/plugin/details/311676
.. _`30 second overview video`: http://www.youtube.com/watch?v=7Eeq_87y3NM

Development Guide
=================

Issue Tracking
--------------

+-----------------------+----------------------------------------+
| Public facing:        | https://jira.atlassian.com/browse/DCON |
+-----------------------+----------------------------------------+
| Internal development: | https://jdog.jira-dev.com/browse/BBC   |
+-----------------------+----------------------------------------+

Development Branches
--------------------
Basically new work should go on master.

+-----------------+-------------------------------------+-------------------------+
|Branch           | Versions                            | Supported JIRA Versions |
+=================+=====================================+=========================+
| default         | 2.3.x                               | 6.5 and above           |
+-----------------+-------------------------------------+-------------------------+
| jira6.4.x       | 2.2.x                               | 6.4                     |
+-----------------+-------------------------------------+-------------------------+
| jira6.3.x       | 2.1.x                               | 6.3.x                   |
+-----------------+-------------------------------------+-------------------------+
| jira6.2.x       | 2.0.x                               | 6.2.1 ~ 6.2.x           |
+-----------------+-------------------------------------+-------------------------+
| jira5.2.x-6.2   | 1.4.x (excluding 1.4.16 and 1.4.17) | 5.2.x ~ 6.2             |
+-----------------+-------------------------------------+-------------------------+
| jira4.x         | ~ 0.15.13                           | 4.x                     |
+-----------------+-------------------------------------+-------------------------+

Building the code
========
The integration tests call a specific user account in Bitbucket whose credentials are not available, also they take around an hour to run. If you want to compile the code you can use mvn clean install -DskipITs=true which will run the unit test suite.