from fastapi import FastAPI
import py_eureka_client.eureka_client as eureka_client
import socket

app = FastAPI()

@app.on_event("startup")
async def register_to_eureka():
    ip = socket.gethostbyname(socket.gethostname())
    await eureka_client.init_async(
        eureka_server="http://192.168.1.21:8761/eureka/",
        app_name="python-service",
        instance_port=8000,
        instance_host=ip
    )

@app.get("/hello")
def read_hello():
    return {"message": "hello"}