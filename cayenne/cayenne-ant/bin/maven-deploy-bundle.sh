#!/bin/sh

# Based on deploy-bundle from Maven:
#   http://svn.apache.org/repos/asf/maven/components/trunk/maven-meeper/src/bin/deploy-bundle

BUNDLEURL=$1
GROUPID=$2
VERSION=$3

[ "${BUNDLEURL}" = "" ] && echo && echo "You must specify a bundle URL!" && echo && exit

WORKDIR=bundle.tmp
# repo dir relative to WORKDIR
REPODIR=/var/sites/objectstyle/html/maven2

rm -rf $WORKDIR > /dev/null 2>&1

mkdir $WORKDIR

cd $WORKDIR

wget $BUNDLEURL
BUNDLE=`echo $BUNDLEURL | sed -e 's#^.*/##;'`

echo $BUNDLE

(

  jar xf $BUNDLE

  # copy files in subdirs to workdir
  for d in `find ./* -type d` ; do
    for f in `find $d -type f` ; do cp $f .; done;
  done

  POM=project.xml
  if [ ! -f ${POM} ]
  then
    POM=pom.xml
  fi
  if [ ! -f ${POM} ]
  then
    POM=`find . -iname *.pom`
  fi

  [ ! -f ${POM} ] && echo && echo "Cannot deploy without the pom.xml or project.xml file!" && echo && exit

  less $POM

  ../d2u ${POM}

  if [ ! -z $VERSION ]
  then
    version=$VERSION
  else
    version=`cat ${POM} | tr '\n' ' ' | sed 's#<build>.*</build>##' | sed 's#<versions>.*</versions>##' | sed 's#<dependencies>.*</dependencies>##' | sed 's#<reporting>.*</reporting>##' | grep '<version>' | sed -e 's#^.*<version>##;s#</version>.*$##'`

    if [ -z $version ]
    then
      version=`grep currentVersion ${POM} | sed -e 's/^ *//;s/<currentVersion>//;s/<\/currentVersion>//'`
    fi
  fi

  artifactId=`cat ${POM} | tr '\n' ' ' | sed 's#<build>.*</build>##' | sed 's#<dependencies>.*</dependencies>##' | sed 's#<contributors>.*</contributors>##' | sed 's#<dependencies>.*</dependencies>##' | sed 's#<reporting>.*</reporting>##' | grep '<artifactId>' | sed -e 's#^.*<artifactId>##;s#</artifactId>.*$##'`

  if [ -z $artifactId ]
  then
    artifactId=`cat ${POM} | tr '\n' ' ' | sed 's#<build>.*</build>##' | sed 's#<versions>.*</versions>##' | sed 's#<developers>.*</developers>##' | sed 's#<dependencies>.*</dependencies>##' | sed 's#<reporting>.*</reporting>##' | sed 's#<contributors>.*</contributors>##' | grep '<id>' | sed -e 's#^.*<id>##;s#</id>.*$##'`
  fi
  
  if [ ! -z $GROUPID ]
  then
    groupId=${GROUPID}
  else
    groupId=`cat ${POM} | tr '\n' ' ' | sed 's#<build>.*</build>##' | sed 's#<dependencies>.*</dependencies>##' | sed 's#<reporting>.*</reporting>##' | grep '<groupId>' | sed -e 's#^.*<groupId>##;s#</groupId>.*$##'`  

    if [ -z $groupId ]
    then
      groupId=${artifactId}
    fi
  fi

  version=`echo ${version} | sed -e 's/ *$//'`
  artifactId=`echo ${artifactId} | sed -e 's/ *$//'`
  groupId=`echo ${groupId} | sed -e 's/ *$//'`  

  echo
  echo "   version: [${version}]"
  echo "   groupId: [${groupId}]"  
  echo "artifactId: [${artifactId}]"
  echo

  if [ -d $REPODIR/${groupId} ]
  then
    echo "The group already exists"
  else
    echo "The group does NOT already exist"
  fi

  echo
  echo -n Hit Enter to continue or Ctrl-C to abort...
  read

  LIC=LICENSE.txt

  # A little help for manually created upload bundles
  [ -f license.txt ] && mv license.txt $LIC

  if [ -f $LIC ]
  then
    ../d2u $LIC
    mkdir -p $REPODIR/${groupId}/licenses
    cp -i $LIC $REPODIR/${groupId}/licenses/${artifactId}-${version}.license  
  fi

  cp ${POM} ${artifactId}-${version}.pom


  mkdir -p $REPODIR/${groupId}/poms
  cp -i ${artifactId}-${version}.pom $REPODIR/${groupId}/poms

  artifactType=`echo ${artifactId} | sed -e 's/maven-.*-plugin//'`
  
  if [ -z ${artifactType} ]
  then
    echo "Deploying Plugin ..."
     mkdir -p $REPODIR/${groupId}/plugins
     cp -i ${artifactId}-${version}.jar $REPODIR/${groupId}/plugins
  else
    echo "Deploying JAR ..."
    mkdir -p $REPODIR/${groupId}/jars
    cp -i ${artifactId}-${version}.jar $REPODIR/${groupId}/jars
  fi
  
  if [ -f ${artifactId}-${version}-sources.jar ]
  then
    echo "Deploying Java sources ..."
    mkdir -p $REPODIR/${groupId}/java-sources
    cp -i ${artifactId}-${version}-sources.jar $REPODIR/${groupId}/java-sources
  else
    echo "No Java sources available in upload bundle, skipping ..."
  fi

  if [ -f ${artifactId}-${version}-javadoc.jar ]
  then
    echo "Deploying Javadocs ..."
    mkdir -p $REPODIR/${groupId}/javadoc
    cp -i ${artifactId}-${version}-javadoc.jar $REPODIR/${groupId}/javadoc
  else
    echo "No Javadocs available in upload bundle, skipping ..."
  fi

)  
