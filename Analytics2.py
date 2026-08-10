import os
import csv
import re

def find_all_csv_files(folder="Weather"):
    """Найти все CSV файлы в папке"""
    csv_files = []
    for root, dirs, files in os.walk(folder):
        for file in files:
            if file.endswith('.csv'):
                csv_files.append(os.path.join(root, file))
    return csv_files

def extract_date_from_path(filepath):
    """Извлечь дату из пути файла"""
    year = month = day = None
    
    # Разделяем путь
    parts = filepath.split(os.sep)
    
    # Ищем год
    for part in parts:
        if part.isdigit() and len(part) == 4:
            y = int(part)
            if 2012 <= y <= 2015:
                year = y
                break
    
    # Из имени файла
    filename = os.path.basename(filepath)
    
    # Паттерны для дат
    patterns = [
        r'(\d{4})[-_](\d{1,2})[-_](\d{1,2})',  # 2014-5-1
        r'(\d{1,2})[-_](\d{1,2})[-_](\d{4})',  # 1-5-2014
    ]
    
    for pattern in patterns:
        match = re.search(pattern, filename)
        if match:
            try:
                groups = match.groups()
                if len(groups[0]) == 4:  # Год первый
                    y, m, d = map(int, groups)
                else:  # Проверяем что первое число
                    if int(groups[0]) > 12:  # Это день
                        d, m, y = map(int, groups)
                    else:  # Это месяц
                        m, d, y = map(int, groups)
                
                if not year: year = y
                if not month: month = m
                day = d
                break
            except:
                continue
    
    # Если месяц не найден, ищем в пути
    if not month:
        for part in parts:
            if part.isdigit() and 1 <= int(part) <= 12:
                month = int(part)
                break
    
    return year, month, day

def load_all_weather_data():
    """Загрузить все данные о погоде"""
    print("Loading weather data...")
    
    all_data = []
    csv_files = find_all_csv_files()
    
    if not csv_files:
        print("No CSV files found in 'Weather' folder!")
        return []
    
    print(f"Found {len(csv_files)} CSV files")
    
    for filepath in csv_files:
        year, month, day = extract_date_from_path(filepath)
        
        if not year:
            continue
        
        try:
            with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                # Читаем весь файл
                content = f.read()
                lines = content.split('\n')
                
                if not lines:
                    continue
                
                # Определяем разделитель
                first_line = lines[0]
                if '\t' in first_line:
                    delimiter = '\t'
                else:
                    delimiter = ','
                
                # Разбираем CSV
                reader = csv.reader([line for line in lines if line], delimiter=delimiter)
                rows = list(reader)
                
                if len(rows) < 2:
                    continue
                
                header = rows[0]
                
                # Находим индексы колонок
                col_indices = {
                    'time': 0,  # По умолчанию время в первой колонке
                    'temperature': None,
                    'wind_speed': None,
                    'sea_level': None,
                    'humidity': None
                }
                
                # Ищем нужные колонки
                for i, col in enumerate(header):
                    col_lower = str(col).lower()
                    
                    if 'temp' in col_lower:
                        col_indices['temperature'] = i
                    elif 'wind' in col_lower and 'speed' in col_lower:
                        col_indices['wind_speed'] = i
                    elif ('sea' in col_lower and 'level' in col_lower) or ('pressure' in col_lower):
                        col_indices['sea_level'] = i
                    elif 'humid' in col_lower:
                        col_indices['humidity'] = i
                    elif 'time' in col_lower:
                        col_indices['time'] = i
                
                # Читаем данные
                for row in rows[1:]:
                    if len(row) < 2:
                        continue
                    
                    record = {
                        'year': year,
                        'month': month,
                        'day': day,
                        'file': os.path.basename(filepath),
                        'time': '',
                        'temperature': None,
                        'wind_speed': None,
                        'sea_level': None,
                        'humidity': None
                    }
                    
                    # Время
                    if col_indices['time'] is not None and len(row) > col_indices['time']:
                        record['time'] = row[col_indices['time']].strip()
                    
                    # Температура
                    if col_indices['temperature'] is not None and len(row) > col_indices['temperature']:
                        temp_str = row[col_indices['temperature']].strip()
                        if temp_str and temp_str not in ['', 'N/A', '-', 'NA']:
                            try:
                                record['temperature'] = float(temp_str)
                            except:
                                pass
                    
                    # Скорость ветра
                    if col_indices['wind_speed'] is not None and len(row) > col_indices['wind_speed']:
                        wind_str = row[col_indices['wind_speed']].strip()
                        if wind_str and wind_str not in ['', 'N/A', '-', 'NA']:
                            try:
                                record['wind_speed'] = float(wind_str)
                            except:
                                pass
                    
                    # Уровень моря (давление)
                    if col_indices['sea_level'] is not None and len(row) > col_indices['sea_level']:
                        sea_str = row[col_indices['sea_level']].strip()
                        if sea_str and sea_str not in ['', 'N/A', '-', 'NA']:
                            try:
                                record['sea_level'] = float(sea_str)
                            except:
                                pass
                    
                    # Влажность
                    if col_indices['humidity'] is not None and len(row) > col_indices['humidity']:
                        humid_str = row[col_indices['humidity']].strip()
                        if humid_str and humid_str not in ['', 'N/A', '-', 'NA']:
                            try:
                                record['humidity'] = float(humid_str)
                            except:
                                pass
                    
                    # Добавляем запись, если есть хотя бы одно значение
                    if any(record[key] is not None for key in ['temperature', 'wind_speed', 'sea_level', 'humidity']):
                        all_data.append(record)
                                
        except Exception as e:
            print(f"Error reading {filepath}: {e}")
            continue
    
    print(f"Loaded {len(all_data)} weather records")
    
    # Проверяем, какие данные доступны
    available_metrics = []
    if any(d['temperature'] is not None for d in all_data):
        available_metrics.append('temperature')
    if any(d['wind_speed'] is not None for d in all_data):
        available_metrics.append('wind speed')
    if any(d['sea_level'] is not None for d in all_data):
        available_metrics.append('sea level pressure')
    if any(d['humidity'] is not None for d in all_data):
        available_metrics.append('humidity')
    
    if available_metrics:
        print(f"Available metrics: {', '.join(available_metrics)}")
    
    # Показываем доступные годы
    years = sorted(set(d['year'] for d in all_data if d['year']))
    if years:
        print(f"Available years: {', '.join(map(str, years))}")
    
    return all_data

def get_available_years(data):
    """Получить список доступных лет"""
    years = sorted(set(d['year'] for d in data if d['year']))
    return years

def get_available_months(data, year):
    """Получить список доступных месяцев для года"""
    months = sorted(set(d['month'] for d in data if d['year'] == year and d['month']))
    return months

def get_available_days(data, year, month):
    """Получить список доступных дней"""
    days = sorted(set(d['day'] for d in data if d['year'] == year and d['month'] == month and d['day']))
    return days

def filter_data(data, year=None, month=None, day=None):
    """Отфильтровать данные"""
    filtered = []
    for d in data:
        if year and d['year'] != year:
            continue
        if month and d['month'] != month:
            continue
        if day and d['day'] != day:
            continue
        filtered.append(d)
    return filtered

def find_extreme(data, metric, mode='min'):
    """Найти минимальное или максимальное значение метрики"""
    if not data:
        return None
    
    # Фильтруем записи с доступной метрикой
    valid_data = [d for d in data if d[metric] is not None]
    
    if not valid_data:
        return None
    
    if mode == 'min':
        return min(valid_data, key=lambda x: x[metric])
    else:  # max
        return max(valid_data, key=lambda x: x[metric])

def calculate_average(data, metric):
    """Рассчитать среднее значение метрики"""
    if not data:
        return None
    
    # Фильтруем записи с доступной метрикой
    valid_data = [d for d in data if d[metric] is not None]
    
    if not valid_data:
        return None
    
    total = sum(d[metric] for d in valid_data)
    return total / len(valid_data)

def format_date(day, month, year):
    """Форматировать дату в ДД/ММ/ГГГГ"""
    if day and month:
        return f"{day:02d}/{month:02d}/{year}"
    elif month:
        return f"{month:02d}/{year}"
    else:
        return str(year)

def get_metric_info(metric):
    """Получить информацию о метрике"""
    metric_info = {
        'temperature': {
            'name': 'Temperature',
            'unit': '°F',
            'display_name': 'TEMPERATURE'
        },
        'wind_speed': {
            'name': 'Wind Speed',
            'unit': 'MPH',
            'display_name': 'WIND SPEED'
        },
        'sea_level': {
            'name': 'Sea Level Pressure',
            'unit': 'inHg',
            'display_name': 'SEA LEVEL PRESSURE'
        },
        'humidity': {
            'name': 'Humidity',
            'unit': '%',
            'display_name': 'HUMIDITY'
        }
    }
    return metric_info.get(metric, {'name': metric, 'unit': '', 'display_name': metric.upper()})

def display_result(metric, mode, result, year, month=None, day=None):
    """Отобразить результат"""
    metric_info = get_metric_info(metric)
    
    print("\n" + "=" * 50)
    print(f"{mode.upper()} {metric_info['display_name']}")
    print("=" * 50)
    
    # Форматируем период
    if day and month:
        period_str = f"{day:02d}/{month:02d}/{year}"
    elif month:
        month_names = {
            1: 'January', 2: 'February', 3: 'March', 4: 'April',
            5: 'May', 6: 'June', 7: 'July', 8: 'August',
            9: 'September', 10: 'October', 11: 'November', 12: 'December'
        }
        month_name = month_names.get(month, f'Month {month}')
        period_str = f"{month_name} {year}"
    else:
        period_str = f"{year}"
    
    print(f"Period: {period_str}")
    
    if mode == 'average':
        print(f"{metric_info['name']}: {result:.2f}{metric_info['unit']}")
    else:
        print(f"{metric_info['name']}: {result[metric]}{metric_info['unit']}")
        print(f"Time: {result['time']}")
        
        if day and month:
            print(f"Date: {day:02d}/{month:02d}/{year}")
        else:
            actual_date = format_date(result.get('day'), result.get('month'), result.get('year'))
            if actual_date != str(year):
                print(f"Date: {actual_date}")
    
    print("=" * 50)

def check_metric_available(data, metric):
    """Проверить, доступна ли метрика в данных"""
    return any(d[metric] is not None for d in data)

def main():
    print("=" * 50)
    print("WEATHER DATA ANALYZER")
    print("=" * 50)
    
    # Загружаем данные
    data = load_all_weather_data()
    
    if not data:
        print("\nNo data found! Please check:")
        print("1. 'Weather' folder exists in current directory")
        print("2. It contains CSV files with weather data")
        input("\nPress Enter to exit...")
        return
    
    while True:
        print("\n" + "=" * 50)
        print("SELECT WEATHER METRIC")
        print("=" * 50)
        print("What would you like to analyze?")
        
        metrics = []
        metric_options = []
        
        # Проверяем доступные метрики
        if check_metric_available(data, 'temperature'):
            metrics.append('temperature')
            metric_options.append("1. Temperature (°F)")
        
        if check_metric_available(data, 'wind_speed'):
            metrics.append('wind_speed')
            metric_options.append("2. Wind Speed (MPH)")
        
        if check_metric_available(data, 'sea_level'):
            metrics.append('sea_level')
            metric_options.append("3. Sea Level Pressure (inHg)")
        
        if check_metric_available(data, 'humidity'):
            metrics.append('humidity')
            metric_options.append("4. Humidity (%)")
        
        if not metrics:
            print("\nNo weather metrics found in the data!")
            input("Press Enter to exit...")
            return
        
        metric_options.append("5. Exit")
        
        for option in metric_options:
            print(option)
        
        choice = input("\nEnter choice (1-5): ").strip()
        
        if choice == '5':
            print("\nGoodbye!")
            break
        
        # Преобразуем выбор в метрику
        metric_map = {}
        for i, metric in enumerate(metrics, 1):
            metric_map[str(i)] = metric
        
        if choice not in metric_map:
            print("\nInvalid choice!")
            continue
        
        selected_metric = metric_map[choice]
        metric_info = get_metric_info(selected_metric)
        
        print(f"\nSelected: {metric_info['name']}")
        
        while True:
            print("\n" + "=" * 50)
            print(f"{metric_info['display_name']} ANALYSIS")
            print("=" * 50)
            print("What would you like to find?")
            print("1. Minimum value")
            print("2. Maximum value")
            print("3. Average value")
            print("4. Back to metric selection")
            
            analysis_choice = input("\nEnter choice (1-4): ").strip()
            
            if analysis_choice == '4':
                break
            elif analysis_choice not in ['1', '2', '3']:
                print("\nInvalid choice!")
                continue
            
            # Выбираем период
            print("\nSelect time period:")
            print("1. For a specific YEAR")
            print("2. For a specific MONTH")
            print("3. For a specific DAY")
            
            period = input("\nEnter choice (1-3): ").strip()
            
            if period not in ['1', '2', '3']:
                print("\nInvalid choice!")
                continue
            
            # Получаем доступные годы
            years = get_available_years(data)
            if not years:
                print("\nNo year data available!")
                continue
            
            # Выбор года
            print("\nAvailable years:")
            for i, year in enumerate(years, 1):
                print(f"{i}. {year}")
            
            try:
                year_idx = int(input(f"\nSelect year (1-{len(years)}): ")) - 1
                selected_year = years[year_idx]
            except:
                print("\nInvalid selection!")
                continue
            
            selected_month = None
            selected_day = None
            
            if period in ['2', '3']:
                # Выбор месяца
                months = get_available_months(data, selected_year)
                if not months:
                    print(f"\nNo month data for {selected_year}!")
                    continue
                
                print(f"\nAvailable months for {selected_year}:")
                month_names = {
                    1: 'January', 2: 'February', 3: 'March', 4: 'April',
                    5: 'May', 6: 'June', 7: 'July', 8: 'August',
                    9: 'September', 10: 'October', 11: 'November', 12: 'December'
                }
                
                for i, month in enumerate(months, 1):
                    name = month_names.get(month, f'Month {month}')
                    print(f"{i}. {name}")
                
                try:
                    month_idx = int(input(f"\nSelect month (1-{len(months)}): ")) - 1
                    selected_month = months[month_idx]
                except:
                    print("\nInvalid selection!")
                    continue
            
            if period == '3':
                # Выбор дня
                days = get_available_days(data, selected_year, selected_month)
                if not days:
                    print(f"\nNo day data for {selected_year}-{selected_month}!")
                    continue
                
                print(f"\nAvailable days for {selected_year}-{selected_month}:")
                print(f"Days: {', '.join(map(str, days))}")
                
                try:
                    selected_day = int(input(f"\nSelect day (1-{max(days)}): "))
                    if selected_day not in days:
                        print(f"\nDay {selected_day} not available!")
                        continue
                except:
                    print("\nInvalid selection!")
                    continue
            
            # Фильтруем данные
            filtered = filter_data(data, selected_year, selected_month, selected_day)
            
            if not filtered:
                print("\nNo data found for selected criteria!")
                continue
            
            # Выполняем запрос и показываем результат
            if analysis_choice == '1':
                result = find_extreme(filtered, selected_metric, 'min')
                if result:
                    display_result(selected_metric, 'minimum', result, selected_year, selected_month, selected_day)
                else:
                    print(f"\nNo {metric_info['name'].lower()} data found for selected period!")
            
            elif analysis_choice == '2':
                result = find_extreme(filtered, selected_metric, 'max')
                if result:
                    display_result(selected_metric, 'maximum', result, selected_year, selected_month, selected_day)
                else:
                    print(f"\nNo {metric_info['name'].lower()} data found for selected period!")
            
            else:  # analysis_choice == '3'
                avg_value = calculate_average(filtered, selected_metric)
                if avg_value is not None:
                    display_result(selected_metric, 'average', avg_value, selected_year, selected_month, selected_day)
                else:
                    print(f"\nNo {metric_info['name'].lower()} data found for selected period!")
            
            # Спрашиваем, хочет ли пользователь продолжить с этой метрикой
            while True:
                again = input(f"\nWould you like another {metric_info['name'].lower()} analysis? (y/n): ").strip().lower()
                if again == 'y':
                    break
                elif again == 'n':
                    # Возвращаемся к выбору метрики
                    analysis_choice = '4'
                    break
                else:
                    print("Please enter 'y' or 'n'")
            
            if analysis_choice == '4':
                break

# Запускаем программу
if __name__ == "__main__":
    main()