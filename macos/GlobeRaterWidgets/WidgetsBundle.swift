import SwiftUI
import WidgetKit

@main
struct GlobeRaterWidgetsBundle: WidgetBundle {
    var body: some Widget {
        ConverterWidget()
        ConverterBudgetWidget()
        BudgetWidget()
        TravelClockWidget()
        WeatherWidget()
    }
}
