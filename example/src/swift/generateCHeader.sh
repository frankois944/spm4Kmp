#!/bin/bash

PACKAGE_NAME="MyLibrary"
HEADER_FILE="$PACKAGE_NAME.h"

echo "Generating $HEADER_FILE..."

# Write header guards
echo "#ifndef ${PACKAGE_NAME}_H" > $HEADER_FILE
echo "#define ${PACKAGE_NAME}_H" >> $HEADER_FILE
echo "" >> $HEADER_FILE
echo "#ifdef __cplusplus" >> $HEADER_FILE
echo 'extern "C" {' >> $HEADER_FILE
echo "#endif" >> $HEADER_FILE
echo "" >> $HEADER_FILE

# Extract `@_cdecl` functions from Swift files
grep -h "@_cdecl" nativeLinuxShared/*.swift | while read -r line; do
    FUNC_NAME=$(echo "$line" | sed -n 's/@_cdecl("\(.*\)")/\1/p')
    FUNC_SIGNATURE=$(grep -A1 "$line" nativeLinuxShared/*.swift | tail -n 1)

    # Convert Swift function signature to C format
    RETURN_TYPE="void" # Default return type (change manually if needed)
    if [[ "$FUNC_SIGNATURE" == *"->"* ]]; then
        RETURN_TYPE="int" # You can improve this to detect other return types
    fi

    FUNC_ARGS=$(echo "$FUNC_SIGNATURE" | sed -E 's/.*\((.*)\).*/\1/')
    FUNC_ARGS_C=$(echo "$FUNC_ARGS" | sed -E 's/Int32/int/g; s/Float/float/g; s/Double/double/g; s/String/const char*/g')

    echo "$RETURN_TYPE $FUNC_NAME($FUNC_ARGS_C);" >> $HEADER_FILE
done

echo "" >> $HEADER_FILE
echo "#ifdef __cplusplus" >> $HEADER_FILE
echo "}" >> $HEADER_FILE
echo "#endif" >> $HEADER_FILE
echo "" >> $HEADER_FILE
echo "#endif /* ${PACKAGE_NAME}_H */" >> $HEADER_FILE

echo "✅ Header file generated: $HEADER_FILE"
