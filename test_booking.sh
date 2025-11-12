#!/bin/bash
echo "Testing Booking Activity..."
echo "Checking if the Booking.kt file has been updated correctly..."

# Check if the file exists
if [ -f "/Users/musawenkosibhebhe/Desktop/Phambili-Ma-Africa-App-main/app/src/main/java/com/example/phambili_ma_africa/Booking.kt" ]; then
    echo "✅ Booking.kt file exists"
else
    echo "❌ Booking.kt file not found"
    exit 1
fi

# Check if the key changes have been made
if grep -q "serviceTypeSpinner = findViewById(R.id.service_frequency)" "/Users/musawenkosibhebhe/Desktop/Phambili-Ma-Africa-App-main/app/src/main/java/com/example/phambili_ma_africa/Booking.kt"; then
    echo "✅ Service frequency spinner is correctly referenced"
else
    echo "❌ Service frequency spinner reference not found"
fi

if grep -q "bookingTimeInput: Spinner" "/Users/musawenkosibhebhe/Desktop/Phambili-Ma-Africa-App-main/app/src/main/java/com/example/phambili_ma_africa/Booking.kt"; then
    echo "✅ Booking time input is correctly defined as a Spinner"
else
    echo "❌ Booking time input is not defined as a Spinner"
fi

if grep -q "bookingAddressInput = findViewById(R.id.address_street)" "/Users/musawenkosibhebhe/Desktop/Phambili-Ma-Africa-App-main/app/src/main/java/com/example/phambili_ma_africa/Booking.kt"; then
    echo "✅ Address street is correctly referenced"
else
    echo "❌ Address street reference not found"
fi

if grep -q "instructionsInput = findViewById(R.id.special_requests)" "/Users/musawenkosibhebhe/Desktop/Phambili-Ma-Africa-App-main/app/src/main/java/com/example/phambili_ma_africa/Booking.kt"; then
    echo "✅ Special requests is correctly referenced"
else
    echo "❌ Special requests reference not found"
fi

if grep -q "bookingTimeInput.selectedItem" "/Users/musawenkosibhebhe/Desktop/Phambili-Ma-Africa-App-main/app/src/main/java/com/example/phambili_ma_africa/Booking.kt"; then
    echo "✅ Booking time input is correctly accessed as a Spinner"
else
    echo "❌ Booking time input is not accessed as a Spinner"
fi

echo "Test completed."
