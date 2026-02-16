create a new bnd feature for support of parsing of eclipse products

sample eclipse product snippet in a content.xml looks like this 
Here is an example p2 content repository xml snippet for product.
```xml
    <unit id='org.eclipse.platform.ide' version='4.38.0.I20251201-0920'>
      <update id='org.eclipse.platform.ide' range='[4.0.0,4.38.0.I20251201-0920)' severity='0' description='An update for 4.x generation Eclipse Platform.'/>
      <properties size='5'>
        <property name='org.eclipse.equinox.p2.name' value='Eclipse Platform'/>
        <property name='org.eclipse.equinox.p2.type.product' value='true'/>
        <property name='org.eclipse.equinox.p2.type.group' value='true'/>
        <property name='org.eclipse.equinox.p2.description' value='4.38 Release of the Eclipse Platform.'/>
        <property name='org.eclipse.equinox.p2.provider' value='Eclipse.org'/>
      </properties>
      <provides size='1'>
        <provided namespace='org.eclipse.equinox.p2.iu' name='org.eclipse.platform.ide' version='4.38.0.I20251201-0920'/>
      </provides>
      <requires size='8'>
        <required namespace='org.eclipse.equinox.p2.iu' name='org.eclipse.platform.feature.group' range='[4.38.0.v20251201-0920,4.38.0.v20251201-0920]'/>
        <required namespace='org.eclipse.equinox.p2.iu' name='org.eclipse.equinox.p2.user.ui.feature.group' range='[2.4.3000.v20251124-1504,2.4.3000.v20251124-1504]'/>
        <required namespace='org.eclipse.equinox.p2.iu' name='toolingorg.eclipse.platform.ide.application' range='[4.38.0.I20251201-0920,4.38.0.I20251201-0920]'/>
        <required namespace='org.eclipse.equinox.p2.iu' name='tooling.org.eclipse.update.feature.default' range='[1.0.0,1.0.0]'>
          <filter>
            (org.eclipse.update.install.features=true)
          </filter>
        </required>
        <required namespace='org.eclipse.equinox.p2.iu' name='toolingorg.eclipse.platform.ide.configuration' range='[4.38.0.I20251201-0920,4.38.0.I20251201-0920]'/>
        <required namespace='org.eclipse.equinox.p2.iu' name='tooling.source.default' range='[1.0.0,1.0.0]'/>
        <required namespace='org.eclipse.equinox.p2.iu' name='tooling.osgi.bundle.default' range='[1.0.0,1.0.0]'/>
        <required namespace='osgi.ee' name='JavaSE' range='0.0.0'/>
      </requires>
      <touchpoint id='org.eclipse.equinox.p2.osgi' version='1.0.0'/>
      <touchpointData size='1'>
        <instructions size='1'>
          <instruction key='configure'>
            mkdir(path:${installFolder}/dropins);
          </instruction>
        </instructions>
      </touchpointData>
      <licenses size='1'>
        <license uri='http://eclipse.org/legal/epl/notice.php' url='http://eclipse.org/legal/epl/notice.php'>
          Eclipse Foundation Software User Agreement
        </license>
      </licenses>
    </unit>
```

implement inside bundle biz.aQute.repository
use the api package aQute/p2/api
and the provider package aQute/p2/provider/
add testcases for the new implementations validating the parsed index file

use this sample p2 repo as testdata from repo url `file:C:/git/github.com/klibio/example.pde.rcp/releng/repo.binary/target/repository`
There are 2 product units inside. One is named `example-rcp-app-ui-feature` the other one is `example-rcp-app-ui-plugin`
create a expected data for the index file

for parsing of the eclipse products use the capability / requirement model similar to the bundles parsed and stored
create a new capability inside the namespace osgi.identity with the type `org.eclipse.equinox.p2.type.product`
capabilities should contain the eclipse product properties
requirements should be used for the included and required eclipse features and bundles 

generate code, compile and run the testcases and validate they are working