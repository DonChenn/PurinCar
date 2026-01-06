import csv
from datetime import datetime


# --- CSV-BASED FILE FUNCTIONS ---

def read_data_from_csv(file_path):
    """Reads all records from a CSV file and groups them into the car_data dictionary."""
    car_data = {}
    try:
        with open(file_path, 'r', newline='', encoding='utf-8') as f:
            reader = csv.reader(f)
            header = next(reader)  # Skip the header row

            for row in reader:
                record_type, date_str, miles_str = row
                # Recreate the record string format used by the rest of the script
                reconstructed_record = f"Date: {date_str}, Miles: {miles_str}"

                # Group records by their type (e.g., 'Miles', 'Engine Oil')
                if record_type not in car_data:
                    car_data[record_type] = []
                car_data[record_type].append(reconstructed_record)

    except FileNotFoundError:
        # If the file doesn't exist, return an empty dictionary
        return {}
    except StopIteration:
        # Handles the case of an empty file
        return {}
    return car_data


def write_data_to_csv(file_path, car_data):
    """Writes the entire car_data dictionary to a CSV file."""
    with open(file_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        # Write the header
        writer.writerow(['Type', 'Date', 'Mileage'])

        # Iterate through the dictionary and write each record to a new row
        for record_type, records in sorted(car_data.items()):
            for record_str in records:
                service_date, service_miles = _parse_record(record_str)
                if service_date and service_miles is not None:
                    formatted_date = service_date.strftime('%Y-%m-%d')
                    writer.writerow([record_type, formatted_date, service_miles])


# --- HELPER & CORE LOGIC FUNCTIONS ---

def _parse_record(record_str):
    """Helper function to parse a service record string into date and miles."""
    try:
        parts = [p.strip() for p in record_str.split(',')]
        date_str = parts[0].split(': ')[1]
        miles_str = parts[1].split(': ')[1]
        service_date = datetime.strptime(date_str, "%Y-%m-%d")
        service_miles = int(miles_str)
        return service_date, service_miles
    except (ValueError, IndexError):
        return None, None


def options(car_info):
    """Main menu loop for user interaction."""
    while True:
        option = input("\nWhat do you want to do?\n"
                       "1: Update miles\n"
                       "2: Update service records\n"
                       "3: View maintenance\n"
                       "4: Exit\n"
                       "Enter your choice: ")
        if option == "1":
            update_miles(car_info)
        elif option == "2":
            update_service(car_info)
        elif option == "3":
            view_maintenance(car_info)
        elif option == "4":
            print("Exiting program.")
            break
        else:
            print("Invalid option. Please choose a number from 1 to 5.")


def update_miles(car_data):
    """Updates the car's mileage by appending a new record with the current date."""
    last_miles = 0
    if car_data.get('Miles'):
        _, last_miles_val = _parse_record(car_data['Miles'][-1])
        if last_miles_val is not None:
            last_miles = last_miles_val

    # This line automatically gets the current date
    date_input = datetime.now().strftime("%Y-%m-%d")

    while True:
        miles_input = input("What is your current odometer reading (in miles)?\n"
                            "Type 'exit' to return to the main menu: ")
        if miles_input.lower() == "exit":
            return
        try:
            new_miles = int(miles_input)
            if new_miles < last_miles:
                print(f"Error: New mileage ({new_miles}) cannot be less than the last recorded mileage ({last_miles}).")
                continue

            # The date is combined with the new mileage here
            new_record = f"Date: {date_input}, Miles: {new_miles}"

            if 'Miles' not in car_data:
                car_data['Miles'] = []
            car_data['Miles'].append(new_record)

            print(f"Mileage updated to {new_miles} miles on {date_input}.")
            write_data_to_csv(car_csv_file, car_data)
            return
        except ValueError:
            print("Invalid input. Please enter a whole number for the mileage.")


def update_service(car_data):
    """Updates service records with date and mileage validation."""
    while True:
        print("\nWhich service would you like to update?")
        print("1: Engine Oil", "2: Engine Coolant", "3: Brake Fluid",
              "4: Transmission Fluid", "5: Spark Plugs", "6: Air Filters",
              "7: Battery Fan", "8: Return to main menu", sep="\n")
        service_option = input("Enter your choice: ")
        service_map = {
            "1": "Engine Oil", "2": "Engine Coolant", "3": "Brake Fluid",
            "4": "Transmission Fluid", "5": "Spark Plugs", "6": "Air Filters", "7": "Battery Fan"
        }
        if service_option == "8":
            return
        if service_option not in service_map:
            print("Invalid choice. Please select a number from 1 to 8.")
            continue
        service_name = service_map[service_option]

        while True:
            date_input = input(f"Enter the date of the {service_name} service (YYYY-MM-DD): ")
            try:
                datetime.strptime(date_input, "%Y-%m-%d")
                break
            except ValueError:
                print("Invalid date format. Please use YYYY-MM-DD.")

        current_miles = float('inf')
        if car_data.get('Miles'):
            _, miles_val = _parse_record(car_data['Miles'][-1])
            if miles_val is not None:
                current_miles = miles_val
        else:
            print("Warning: Current mileage is not set. Cannot validate service mileage.")

        last_specific_service_miles = 0
        if car_data.get(service_name):
            last_record_str = car_data[service_name][-1]
            _, last_miles_val = _parse_record(last_record_str)
            if last_miles_val is not None:
                last_specific_service_miles = last_miles_val

        while True:
            miles_input = input(f"Enter the mileage at the time of service: ")
            try:
                service_miles = int(miles_input)
                if service_miles < 0:
                    print("Mileage cannot be negative.")
                elif service_miles <= last_specific_service_miles:
                    print(
                        f"Error: New mileage ({service_miles}) must be greater than the last '{service_name}' service at {last_specific_service_miles} miles.")
                elif service_miles > current_miles:
                    print(
                        f"\nError: Service mileage ({service_miles}) cannot be greater than the current odometer reading ({current_miles}).")
                    print("Canceling operation and returning to the main menu.")
                    return
                else:
                    break
            except ValueError:
                print("Invalid input. Please enter a whole number for mileage.")

        new_record = f"Date: {date_input}, Miles: {service_miles}"
        car_data.setdefault(service_name, []).append(new_record)
        print(f"Service for {service_name} recorded.")
        write_data_to_csv(car_csv_file, car_data)
        return


def view_maintenance(car_data):
    """Displays a report of maintenance status based on predefined intervals."""
    print("\n--- Maintenance Report ---")
    try:
        last_miles_record = car_data['Miles'][-1]
        last_date, current_miles = _parse_record(last_miles_record)
        if current_miles is None:
            raise ValueError("Could not parse the last mileage record.")
        print(f"Current Odometer: {current_miles} miles (as of {last_date.strftime('%Y-%m-%d')})\n")
    except (KeyError, ValueError, IndexError):
        print("Error: Please update your current mileage first.\n")
        return

    intervals = {
        "Engine Oil": {"miles": 5000, "days": 182}, "Engine Coolant": {"miles": 50000, "days": 1825},
        "Brake Fluid": {"miles": 30000, "days": 730}, "Transmission Fluid": {"miles": 60000, "days": 2190},
        "Spark Plugs": {"miles": 120000, "days": 3650}, "Air Filters": {"miles": 35000, "days": 1095},
        "Battery Fan": {"miles": 30000, "days": 730}
    }
    for service, interval in intervals.items():
        if service in car_data and car_data[service]:
            last_record_str = car_data[service][-1]
            last_service_date, last_service_miles = _parse_record(last_record_str)
            if last_service_date and last_service_miles is not None:
                miles_since = current_miles - last_service_miles
                days_since = (datetime.now() - last_service_date).days
                status = "➡️ OK"
                if miles_since >= interval["miles"] or days_since >= interval["days"]:
                    status = "✅ MAINTENANCE DUE"
                print(f"[{status}] {service}:")
                print(f"  - Last Service: {last_service_date.strftime('%Y-%m-%d')} at {last_service_miles} miles")
                print(f"  - Due in {interval['miles'] - miles_since} miles or {interval['days'] - days_since} days.")
                print(f"  - ({miles_since}/{interval['miles']} miles, {days_since}/{interval['days']} days)\n")
            else:
                print(f"Error parsing record for {service}: '{last_record_str}'\n")
        else:
            print(f"No records found for {service}.\n")
    print("---------------------------\n")


if __name__ == "__main__":
    car_csv_file = "car_info.csv"
    try:
        data = read_data_from_csv(car_csv_file)
        if not data:
            print(f"Welcome! Using '{car_csv_file}' for data. A new file will be created when you save.")
    except Exception as e:
        print(f"An error occurred reading the data file: {e}")
        data = {}
    options(data)