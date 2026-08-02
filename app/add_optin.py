file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"

with open(file_path, "r") as f:
    content = f.read()

content = content.replace("class MainViewModel(application: Application) : AndroidViewModel(application) {", 
"@kotlinx.coroutines.FlowPreview\nclass MainViewModel(application: Application) : AndroidViewModel(application) {")

with open(file_path, "w") as f:
    f.write(content)
