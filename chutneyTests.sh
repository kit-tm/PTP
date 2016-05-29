#!/bin/bash

CHUTNEY_DIR="../chutney"
CWD=`pwd`

rm config/testtorrc
ln -s chutneytorrc config/testtorrc

cd $CHUTNEY_DIR
./chutney configure networks/hs
./chutney start networks/hs

cd $CWD

sed -i "s/DirAuthority test000a.*$/$(grep "DirAuthority test000a" $CHUTNEY_DIR/net/nodes/009h/torrc)/" config/testtorrc
sed -i "s/DirAuthority test001a.*$/$(grep "DirAuthority test001a" $CHUTNEY_DIR/net/nodes/009h/torrc)/" config/testtorrc
sed -i "s/DirAuthority test002a.*$/$(grep "DirAuthority test002a" $CHUTNEY_DIR/net/nodes/009h/torrc)/" config/testtorrc

gradle test --info

rm config/testtorrc
ln -s torrc config/testtorrc

cd $CHUTNEY_DIR

./chutney stop networks/hs
