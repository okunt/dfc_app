# dfc_app
      1.	Eksport zawartości dokumentów
        •	Pobieranie zawartości odbędzie się na podstawie listy r_object_ids, która umieszczona bedzie w pliku txt. Lokalizacja pliku będzie zdefiniowana w pliku app.properties.
        •	Pliki będą eksportowane do wyznaczonej lokalizacji, określonej w pliku app.properties
        •	Aplikacja wspierać będzie export plików docx, xlsx oraz pptx. Należy stworzyć klasę odpowiedzialną za mapowanie typów na rozszerzenia plików

      2.	Export propertiesów dokumentów do pliku Excel
        •	Pobieranie propertiesów odbędzie się na podstawie listy r_object_ids, która umieszczona bedzie w pliku txt. Lokalizacja pliku będzie zdefiniowana w pliku app.properties (ta sama jak w punkcie 1)
        •	Plik będzie wyeksportowany do wyznaczonej lokalizacji, określonej w pliku app.properties (ta sama jak w punkcie 1)
        •	Wyeksportowane mają być wszystkie właściwości które zwraca select * from dm_document
        •	Nazwa pliku z eksportem powinna być określona w pliku app.properties

      3.	Masowe przypisywanie permission seta do dokumentów
        •	Przypisywanie permission seta odbędzie się dla plików określonych na podstawie listy r_object_ids, która umieszczona bedzie w pliku txt. Lokalizacja pliku będzie zdefiniowana w pliku app.properties
        (ta sama jak w punkcie 1 i 2)
        •	Przypisywanie permission seta odbędzie się po jego nazwie, podanej w pliku app.properties

        450003e880000101	dm_450003e880000101	dm_450003e880000101	dmadmin
        450003e880000102	dm_450003e880000102	dm_450003e880000102	idm_dev

      4.	Masowe tworzenie permission setów
        •	Dane do tworzenia permission setów zapisane będą w pliku csv jak na poniższym przykładzie (znak separatora jest dowolny):
        object_name|description|owner_name|r_accessor_name[0],r_accessor_permit[0],r_accessor_xpermit[0]|r_accessor_name[1],r_accessor_permit[1],r_accessor_xpermit[1]…………….
        •	Ścieżka do pliku z danymi zdefiniowana będzie w pliku app.properties (inna niż w punktach 1, 2, 3)

       Generalne informacje odnośnie  implementacji:
        o	Aplikacja ma posiadać cztery tryby działania. Każdy wykonywać ma jedną z funkcjonalności. Tryb określony ma być za pomocą właściwości MODE w pliku app.properties. Nazwy trybów oraz wszystkie inne są dowolne.
        o	W plikach z danymi każda linijka ma zawierać pojedyńczy wpis, r_object_id lub definicję permission setu
        o	Wymagane jest zastosowanie podstawowych zasad stosowanych do utrzymania jakości kodu (DRY, SRP)
        o	Dozwolone jest nieograniczone użycie zewnętrznych bibliotek. Wszystkie powinny być zaimportowane do projektu w formie archiwów typu JAR.
        o	Wykonane zadanie należy umieścić w dedykowanym publiczym repozytorium w serwisie github i dostarczyć adres tego repozytorium prowadzącemu do dnia 31.08.2019

