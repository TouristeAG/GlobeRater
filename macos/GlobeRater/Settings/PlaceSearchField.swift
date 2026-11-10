import GlobeKit
import SwiftUI

/// City search on Open-Meteo, with up to 5 suggestions.
struct PlaceSearchField: View {
    let title: String
    let prompt: String
    @Binding var place: Place?
    @State private var query = ""
    @State private var results: [Place] = []
    @State private var notFound = false

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                TextField(text: $query, prompt: Text(verbatim: prompt)) { Text(verbatim: title) }
                if place != nil {
                    Button { place = nil; query = "" } label: { Image(systemName: "xmark.circle.fill") }
                        .buttonStyle(.borderless).foregroundStyle(.secondary)
                }
            }
            if let place, query.isEmpty {
                Text(verbatim: place.label).font(.callout).foregroundStyle(.secondary)
            }
            ForEach(results, id: \.self) { result in
                Button {
                    place = result
                    query = ""
                    results = []
                } label: {
                    Label(result.label, systemImage: "mappin.and.ellipse").frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.borderless)
            }
            if notFound { Text(verbatim: L("mac_city_not_found")).font(.callout).foregroundStyle(.orange) }
        }
        .task(id: query) {
            let q = query.trimmingCharacters(in: .whitespaces)
            notFound = false
            guard q.count >= 2 else { results = []; return }
            try? await Task.sleep(for: .milliseconds(400))
            guard !Task.isCancelled else { return }
            let language = Locale.current.language.languageCode?.identifier ?? "en"
            results = (try? await OpenMeteoClient().search(q, language: language)) ?? []
            notFound = results.isEmpty
        }
    }
}
