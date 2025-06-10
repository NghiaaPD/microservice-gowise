from fastapi import FastAPI
import py_eureka_client.eureka_client as eureka_client
import socket
import os
from prometheus_fastapi_instrumentator import Instrumentator

app = FastAPI()


@app.on_event("startup")
async def register_to_eureka():
    ip = socket.gethostbyname(socket.gethostname())
    eureka_server = os.getenv(
        "EUREKA_SERVER_URL",
        "http://discovery-server:8761/eureka/"
    )
    app_name = os.getenv("APPLICATION_NAME", "python-service")
    instance_port = int(os.getenv("SERVER_PORT", "8001"))
    await eureka_client.init_async(
        eureka_server=eureka_server,
        app_name=app_name,
        instance_port=instance_port,
        instance_host=ip
    )


@app.get("/hello")
def read_hello():
    return {"message": "hello"}


Instrumentator().instrument(app).expose(app)
