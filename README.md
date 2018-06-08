# About
This README provides information and instructions on how to install and 
configure the sample integration between SAP Jam and Atlassian JIRA to support 
IT project work patterns.  The sample demonstrates a bi-directional integration 
by allowing project members to collaborate using SAP Jam while viewing live 
issues and statuses from JIRA.

# What's Included?
* A Maven project for building the plug-in to be installed in JIRA
* Custom group templates to be imported into SAP Jam:
  * IT Project Room Template
  * Issue Escalation Room Template

# Prerequisites
* OpenSSL
* Apache Maven
* Atlassian Plugin SDK
* SAP Jam Enterprise Edition
* SAP Jam Work Pattern Builder

# Getting Started
## Start JIRA
Maven will download the necessary dependencies for JIRA runtime and to build 
the plug-in.
````
    cd jira_plugin (the folder where pom.xml file is)
    atlas-run
````

## Configure Trusts
1. Generate a private key/certificate pair
  1. Run this command in a shell to generate private key/certificate pair. When 
prompted, fill in the attributes for the certificate. Note: The attributes 
are not used by the integration.
````
    openssl req -x509 -newkey rsa:2048 -keyout sap_jam_app_link.key -out sap_jam_app_link.cer -days 365 -nodes
````
2. In the SAP Jam Admin console, go to **OAuth Clients** > **Add OAuth Client** to 
create a new OAuth Client. NOTE: JIRA is an OAuth client of SAP Jam.
  1. Complete all fields but leave the X509 Certificate field blank.
3. In JIRA administration, create a new Application Link
  1. For Outgoing Authentication, copy/paste the OAuth key and secret from SAP Jam
  2. Copy/paste the private key/certificate generated above
4. In the SAP Jam Admin console, go to **SAML Trusted IDPs** > **Register your 
identity provider** and then do the following in the Register a new SAML Trusted 
Identity Provider screen to create a new Trusted IdP. This is to grant trust to 
assertions in API calls from JIRA.
  1. Copy/paste the IdP ID from JIRA
  2. Copy/paste the X509 certificate from JIRA
  3. From the Allowed Assertion Scope drop-down list, choose "Users in my company".
5. In the SAP Jam Admin console, go to **SAML Local Identity Provider**. NOTE: 
SAP Jam generates assertion for JIRA.
  1. If there are no existing Signing Private Key and X509 Certificate, 
click **Generate Key Pair** and then click **Save**.
6. In JIRA administration, configure Application Link (SAP Jam is an OAuth client of JIRA)
  1. For Incoming Authentication, copy/paste from SAP Jam the Local IdP Issuer 
(an URL) as the IdP ID
  2. Copy/paste from SAP Jam the Local IdP certificate
7. In the SAP Jam Admin console, go to **External Applications**. NOTE: Use 
SAP Jam assertion in calls to JIRA.
  1. Click Add Application > Third-party ODATA Source 
  2. Enter a descriptive name for the external application.  Example: My JIRA
  3. Under the "Select Authentication Type" dropdown, select "Per User".  **NOTE: Per User authentication is 
based on user's email address. Ensure the email address in Jam user profile 
matches that of in the JIRA user profile.**
  4. From the JIRA Incoming Authentication screen, copy/paste the Client ID and Client Secret into the OAuth 2.0 Client Id and Secret fields respectively.
  6. Copy/paste from JIRA Incoming Authentication the IdP ID as the Service Provider Name
  7. Scope is left blank.
  8. Select the respective JIRA OAuth client under the "Select Trusted OAuth Client" dropdown (optimize away security callbacks to JIRA if OAuth client is trusted)
  9. Select "SAP Jam" under the "Select the source of the SAML assertion provided by SAP Jam" dropdown.

## Register JIRA Record Types
1. In JIRA administration under External Application that was just created, 
select **Manage Record Types**.
2. Add a new record type for Issues using these values:
````
    External Type: http://hostname/plugins/servlet/sapjam/api/OData/$metadata#Issues
    Annotation: http://hostname/download/resources/com.sap.jam.samples.jira.plugin.jam-plugin/annotations.xml
````
3. Add a new record type for Filters using these values:
````
    External Type: http://hostname/plugins/servlet/sapjam/api/OData/$metadata#Filters
    Annotation: http://hostname/download/resources/com.sap.jam.samples.jira.plugin.jam-plugin/annotations.xml
````

## Configure Plugin Role
1. In JIRA administration, go to **Users** > **Roles** to create a new Role 
named "JamUser".
2. Select the Project, assign to the JamUser role the list or groups of users 
for whom the SAP Jam integration should be enabled

## Import Custom Group Templates
1. In the SAP Jam Admin console, go to **Group Templates** and then click 
**Import a template** to import and enable the custom group templates included 
in the sample.

## How to Verify the Integration is Configured Correctly
If you have already created issues and filters in JIRA, you can verify the 
integration following these guidelines.

1. In SAP Jam, login with a valid user account, click **Business Records** on 
the global navigation bar.
  1. From SAP Jam,  You can select the JIRA external application and browse for Issues.
  2. When you hover over each issue, you can create a group from the issue 
using the Issue Escalation Template.
  3. From SAP Jam, click **Business Records**. You can select the JIRA external 
application and browse for filters
  4. When you hover over each filter, you can create a group from the filter 
using the IT Project Room Template.
  5. If you have Issues matching the Filter criteria, they will be shown on 
the group overview page.
2. In JIRA, login with a valid user account, select an Issue.
  1. Under More Actions, two additional actions are available: "Create SAP Jam 
Group" and "Feature in SAP Jam Group".
  2. The SAP Jam Feed section near the bottom of the Issue page shows the 
activities and comments on this issue.
  3. The SAP Jam Groups panel on the right shows the groups in which this Issue 
is currently shared.

For more information on the business benefits and detailed workflows, consult 
the "SAP Jam IT Project Work Patterns User Guide" included in this sample.

# License
Copyright 2015, SAP SE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
