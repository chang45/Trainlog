import urllib.request
url = "https://repo1.maven.org/maven2/com/volcengine/volcengine-java-sdk-ark-runtime/2.0.36/volcengine-java-sdk-ark-runtime-2.0.36.jar"
out = r"E:\TrainLog\.dsh_tmp\ark-sdk.jar"
urllib.request.urlretrieve(url, out)
print("ok")