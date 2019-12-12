import socket
import sys
import os
import wave
import string
import random
from typing import Optional, Callable, AnyStr

try:
    from thread import start_new_thread
except:
    from _thread import start_new_thread

host = "0.0.0.0"
port = 8080

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

try:
    server.bind((host, port))
except Exception as e:
    print(f"Failed to bind socket {e}")
    sys.exit()

server.listen(10)

print("Server is listening")

def static_vars(**kwargs):
    def decorate(func):
        for k in kwargs:
            setattr(func, k, kwargs[k])
        return func
    return decorate

@static_vars(alphabet = string.ascii_lowercase + string.digits)
def uuid() -> AnyStr:
    return ''.join(random.choices(uuid.alphabet, k=8))

def readFrom(connection: socket, callback: Optional[Callable[[bytearray], None]] = None, eot: Optional[bytes] = None) -> bytearray:
    datas = bytearray()

    while True:
        data = connection.recv(1024)

        if not data:
            break

        datas.extend(data)

        if eot is not None and data.endswith(eot):
            del datas[len(datas) - len(eot) : len(datas)]

            if callback is not None:
                callback(data[0:len(data) - len(eot)])

            break

        if callback is not None:
            callback(data)

    return datas

def handleConnection(connection):
    id = uuid()

    print(f"Connected client {id}")

    action = readFrom(connection, eot = u"\u0004".encode('utf-8')).decode('utf-8').split()

    filename = "unsupported-action.wav"

    if action[0] == "Train":
        maybeName = f"audio_raw_training/{action[1]}s"

        if os.path.isfile(f"{maybeName}.wav"):
            suffix = 0

            while True:
                suffix += 1

                if not os.path.isfile(f"{maybeName}-{suffix}.wav"):
                    maybeName = f"{maybeName}-{suffix}"
                    break

        filename = f"{maybeName}.wav"
    elif action[0] == "Classify":
        filename = f"audio_raw_classification/{id}.wav"

    print(f"Client wants to {action[0].lower()}. Writing to file {filename}.")

    file = wave.open(filename, "w")
    file.setnchannels(1)
    file.setsampwidth(2)
    file.setframerate(44100)
    readFrom(connection, lambda data : file.writeframes(data))

    connection.close()

    print(f"Disconnected client {id}")

while True:
    connection, address = server.accept()

    start_new_thread(handleConnection, (connection,))

server.close()