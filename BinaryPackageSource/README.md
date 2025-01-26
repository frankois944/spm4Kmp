Build a dummy xcframework for testing local binary import.

The xcframework should already be available in the repository and used by the tests.

If you need to update the source, update the code, run `./build.sh` and the framework will be generated in this folder : ../plugin-build/plugin/src/functionalTest/resources

Note: Update the checksum show in the output of the command in the test `build with remote binary xcframework`.
