import threading

from flask import Flask
from prometheus_client import make_wsgi_app
from werkzeug.middleware.dispatcher import DispatcherMiddleware


def start_prometheus_monitoring(host='0.0.0.0', port=5000):
    app = Flask(__name__)
    app.wsgi_app = DispatcherMiddleware(app.wsgi_app, {
        '/metrics': make_wsgi_app()
    })
    threading.Thread(target=lambda: app.run(host=host, port=port, debug=True, use_reloader=False)).start()
