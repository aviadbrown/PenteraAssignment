from argparse import ArgumentParser

import requests


def main(url: str = 'localhost'):
	try:
		# Perform API call to the running container
		response = requests.get(f'http://{url}:8000')
		response_code = response.status_code
	except Exception as e:
		print(f'Connection error: Is the container running? Original error: {e}')
		return 1
	response_body = response.text.replace('"', '')

	# Verify return code and output
	if response_code == 200 and response_body.strip() == "Hello, DevOps!":
		print("API call successful (HTTP 200 OK) and output is correct")
		return 0
	elif response_code == 200:
		print(f"API call successful (HTTP 200 OK) but output is incorrect: {response_body}")
		return 1
	else:
		print(f"API call failed with HTTP status code {response_code}")
		return 1


if __name__ == "__main__":
	parser = ArgumentParser(description='''	change the BUNG backup destination buckets key in consul''')
	parser.add_argument('--url', default="localhost", type=str, help='The url address')
	args = parser.parse_args()
	status = main(url=args.url)
	exit(status)
